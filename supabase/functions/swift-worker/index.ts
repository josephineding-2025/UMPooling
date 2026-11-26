// Deploy with Verify JWT OFF. The Vault x-webhook-secret authenticates requests.
// Requires migrations 6,9,10,11 and server-only Firebase/Webhook secrets.
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.119.2";
import { JWT } from "https://esm.sh/google-auth-library@9.15.1";
import {
    isUnregistered,
    parseEvent,
    requestBody,
    safeEqual,
    sourceKeys,
} from "./notification_logic.ts";

type Notice = {
    userId: string;
    title: string;
    body: string;
    data: Record<string, string>;
    high?: boolean;
};
async function handleRequest(req: Request): Promise<Response> {
    if (req.method !== "POST") {
        return new Response("Method not allowed", { status: 405 });
    }
    const expected = Deno.env.get("WEBHOOK_SECRET");
    if (
        !expected ||
        !safeEqual(req.headers.get("x-webhook-secret") ?? "", expected)
    ) return new Response("Unauthorized", { status: 401 });
    let payload;
    try {
        payload = await req.json();
    } catch {
        return new Response("Invalid JSON", { status: 400 });
    }
    const db = createClient(
        Deno.env.get("SUPABASE_URL")!,
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );
    if (payload?.mode === "retry") {
        const { data: rows, error } = await db.from("push_deliveries").select(
            "event_key",
        )
            .or(`status.eq.failed,and(status.eq.sending,lease_until.lt.${
                new Date().toISOString()
            })`)
            .lt("attempts", 5).order("updated_at").limit(50);
        if (error) return new Response("Retry lookup failed", { status: 503 });
        let failed = 0;
        for (
            const key of [
                ...new Set((rows ?? []).map((row) => row.event_key as string)),
            ].slice(0, 10)
        ) {
            const [table, id] = key.split(":");
            if (!Object.hasOwn(sourceKeys, table)) continue;
            const response = await handleRequest(
                new Request(req.url, {
                    method: "POST",
                    headers: { "x-webhook-secret": expected },
                    body: JSON.stringify({
                        table,
                        record: {
                            [sourceKeys[table as keyof typeof sourceKeys]]: id,
                        },
                    }),
                }),
            );
            if (!response.ok) failed++;
        }
        return Response.json({ failed }, { status: failed ? 503 : 200 });
    }
    let event;
    try {
        event = parseEvent(payload);
    } catch {
        return new Response("Invalid event", { status: 400 });
    }
    const eventKey = `${event.table}:${event.id}`;
    const discard = async () => {
        const { error } = await db.from("push_deliveries").update({
            status: "discarded",
            updated_at: new Date().toISOString(),
        })
            .eq("event_key", eventKey).neq("status", "sent");
        if (error) throw new Error("Delivery cleanup failed");
    };
    // Never trust the supplied record's sender, recipient, body or seat count.
    const { data: record, error: sourceError } = await db.from(event.table)
        .select("*").eq(sourceKeys[event.table], event.id).maybeSingle();
    if (sourceError) {
        return new Response("Source lookup failed", { status: 503 });
    }
    if (!record) {
        try {
            await discard();
        } catch {
            return new Response("Delivery cleanup failed", { status: 503 });
        }
        return new Response("Source no longer exists", { status: 200 });
    }
    const notices: Notice[] = [];
    const baseData = {
        table: event.table,
        [sourceKeys[event.table]]: event.id,
    };
    const must = <T>(result: { data: T; error: unknown }): NonNullable<T> => {
        if (result.error || result.data == null) {
            throw new Error("Database lookup failed");
        }
        return result.data as NonNullable<T>;
    };
    try {
        if (event.table === "bookings") {
            const offer = must(
                await db.from("trip_offers").select("driver_id").eq(
                    "trip_offer_id",
                    record.trip_offer_id,
                ).single(),
            );
            notices.push({
                userId: offer.driver_id,
                title: "New Seat Booked",
                body: `A passenger booked ${record.seats_booked} seat(s).`,
                data: baseData,
            });
        } else if (event.table === "messages") {
            const recipients = new Set<string>();
            if (record.booking_id) {
                const booking = must(
                    await db.from("bookings").select(
                        "passenger_id,trip_offer_id",
                    ).eq("booking_id", record.booking_id).single(),
                );
                const offer = must(
                    await db.from("trip_offers").select("driver_id").eq(
                        "trip_offer_id",
                        booking.trip_offer_id,
                    ).single(),
                );
                if (
                    ![booking.passenger_id, offer.driver_id].includes(
                        record.sender_id,
                    )
                ) throw new Error("Sender is not a participant");
                recipients.add(booking.passenger_id);
                recipients.add(offer.driver_id);
            } else if (record.trip_id) {
                const trip = must(
                    await db.from("trips").select("driver_id").eq(
                        "trip_id",
                        record.trip_id,
                    ).single(),
                );
                const passengers = must(
                    await db.from("bookings").select("passenger_id").eq(
                        "trip_id",
                        record.trip_id,
                    ).in("status", ["confirmed", "completed"]),
                );
                recipients.add(trip.driver_id);
                for (const p of passengers) recipients.add(p.passenger_id);
                if (!recipients.has(record.sender_id)) {
                    throw new Error("Sender is not a participant");
                }
            }
            recipients.delete(record.sender_id);
            for (const userId of recipients) {
                notices.push({
                    userId,
                    title: "New Carpool Message",
                    body: "Open UMPooling to read your message.",
                    data: baseData,
                });
            }
        } else if (event.table === "sos_alerts") {
            const staff = must(
                await db.from("users").select("usersId").eq(
                    "usersRole",
                    "staffAnalyst",
                ),
            );
            for (const u of staff) {
                notices.push({
                    userId: u.usersId,
                    title: "EMERGENCY SOS ALERT",
                    body: "An active trip needs attention. Open UMPooling.",
                    data: { ...baseData, trip_id: record.trip_id },
                    high: true,
                });
            }
        } else {
            const matches = must(
                await db.rpc("find_overlapping_drivers_for_request", {
                    p_request_id: event.id,
                }),
            );
            for (const match of matches ?? []) {
                const co2 = must(
                    await db.rpc("estimate_trip_co2", {
                        p_distance: match.distance_km,
                        p_passengers: record.requested_seats ?? 1,
                        p_vehicle: match.vehicle_type ?? "car",
                        p_fuel: match.fuel_category ?? "petrol",
                    }),
                );
                notices.push({
                    userId: match.driver_id,
                    title: "Ride Request Near Your Route",
                    body: requestBody(record.requested_seats ?? 1, Number(co2)),
                    data: { ...baseData, estimated_co2_kg: String(co2) },
                });
            }
        }
        if (!notices.length) {
            await discard();
            return new Response("No matching recipients", { status: 200 });
        }
        const unique = [...new Map(notices.map((n) => [n.userId, n])).values()];
        const profiles = must(
            await db.from("users").select("usersId,fcm_token").in(
                "usersId",
                unique.map((n) => n.userId),
            ),
        );
        const tokenByUser = new Map(
            profiles.filter((p) => p.fcm_token).map(
                (p) => [p.usersId, p.fcm_token as string],
            ),
        );
        const ready = unique.filter((n) => tokenByUser.has(n.userId));
        if (!ready.length) {
            await discard();
            return new Response("No registered devices", { status: 200 });
        }
        const client = new JWT({
            email: Deno.env.get("FIREBASE_CLIENT_EMAIL"),
            key: Deno.env.get("FIREBASE_PRIVATE_KEY")?.replace(/\\n/g, "\n"),
            scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
        });
        const auth = await client.authorize();
        let sent = 0, failed = 0;
        for (const notice of ready) {
            const claimed = must(
                await db.rpc("claim_push_delivery", {
                    p_event_key: eventKey,
                    p_user_id: notice.userId,
                }),
            );
            if (!claimed) continue;
            const token = tokenByUser.get(notice.userId)!;
            let ok = false;
            try {
                const response = await fetch(
                    `https://fcm.googleapis.com/v1/projects/${
                        Deno.env.get("FIREBASE_PROJECT_ID")
                    }/messages:send`,
                    {
                        method: "POST",
                        signal: AbortSignal.timeout(10000),
                        headers: {
                            Authorization: `Bearer ${auth.access_token}`,
                            "Content-Type": "application/json",
                        },
                        body: JSON.stringify({
                            message: {
                                token,
                                notification: {
                                    title: notice.title,
                                    body: notice.body,
                                },
                                data: notice.data,
                                android: {
                                    priority: notice.high ? "high" : "normal",
                                    notification: { tag: eventKey },
                                },
                            },
                        }),
                    },
                );
                ok = response.ok;
                if (!ok) {
                    const result = await response.json().catch(() => null);
                    // A generic 404 can mean a wrong project, not a stale token.
                    if (isUnregistered(result)) {
                        must(
                            await db.from("users").update({ fcm_token: null })
                                .eq("usersId", notice.userId).eq(
                                    "fcm_token",
                                    token,
                                ).select("usersId"),
                        );
                    }
                }
            } catch {
                /* Retain tokens after transport failures; retry this event safely. */
            }
            must(
                await db.from("push_deliveries").update({
                    status: ok ? "sent" : "failed",
                    updated_at: new Date().toISOString(),
                }).eq("event_key", eventKey).eq("user_id", notice.userId)
                    .select("event_key"),
            );
            if (ok) {
                sent++;
            } else failed++;
        }
        return Response.json({ sent, failed }, { status: failed ? 503 : 200 });
    } catch {
        // Never log tokens, webhook secrets, message contents or source error bodies.
        console.error(
            "Push delivery failed; inspect service configuration and delivery status.",
        );
        return new Response("Delivery failed", { status: 503 });
    }
}
serve(handleRequest);
