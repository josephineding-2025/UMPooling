/** Pure routing helpers: safe to test without Supabase/Firebase credentials. */
export const sourceKeys = {
    bookings: "booking_id",
    messages: "message_id",
    sos_alerts: "alert_id",
    ride_requests: "request_id",
} as const;
export type SourceTable = keyof typeof sourceKeys;
export function parseEvent(value: unknown): { table: SourceTable; id: string } {
    if (!value || typeof value !== "object") throw new Error("Invalid event");
    const event = value as Record<string, unknown>;
    if (
        typeof event.table !== "string" ||
        !Object.hasOwn(sourceKeys, event.table)
    ) throw new Error("Unsupported table");
    const table = event.table as SourceTable;
    const record = event.record as Record<string, unknown> | undefined;
    const id = record?.[sourceKeys[table]];
    if (
        typeof id !== "string" ||
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
            id,
        )
    ) throw new Error("Invalid source ID");
    return { table, id };
}
export function safeEqual(a: string, b: string): boolean {
    if (a.length !== b.length) return false;
    let diff = 0;
    for (let i = 0; i < a.length; i++) {
        diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
    }
    return diff === 0;
}
export function isUnregistered(response: unknown): boolean {
    const value = response as
        | { error?: { details?: { errorCode?: string }[] } }
        | null;
    return value?.error?.details?.some((detail) =>
        detail.errorCode === "UNREGISTERED"
    ) === true;
}
export function requestBody(seats: number, estimatedCo2: number): string {
    return `A request for ${seats} seat(s) matches your route. Estimated CO2 saved: ${
        estimatedCo2.toFixed(2)
    } kg.`;
}
