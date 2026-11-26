import { test } from "node:test";
import assert from "node:assert/strict";
import {
    isUnregistered,
    parseEvent,
    requestBody,
    safeEqual,
} from "./notification_logic.ts";
const id = "00000000-0000-4000-8000-000000000001";
test("webhooks accept only supported source IDs, ignoring forged recipients", () => {
    assert.deepEqual(
        parseEvent({
            table: "ride_requests",
            record: { request_id: id, user_id: "forged" },
        }),
        { table: "ride_requests", id },
    );
    for (
        const value of [null, {}, { table: "users", record: { usersId: id } }, {
            table: "bookings",
            record: { booking_id: "bad" },
        }]
    ) assert.throws(() => parseEvent(value));
});
test("secret equality requires full matching input", () => {
    assert.equal(safeEqual("secret", "secret"), true);
    assert.equal(safeEqual("secret", "secreT"), false);
    assert.equal(safeEqual("secret", "secretx"), false);
});
test("only explicit FCM UNREGISTERED errors invalidate tokens", () => {
    assert.equal(
        isUnregistered({ error: { code: 404, status: "NOT_FOUND" } }),
        false,
    );
    assert.equal(
        isUnregistered({
            error: { details: [{ errorCode: "SENDER_ID_MISMATCH" }] },
        }),
        false,
    );
    assert.equal(
        isUnregistered({ error: { details: [{ errorCode: "UNREGISTERED" }] } }),
        true,
    );
    assert.equal(isUnregistered(null), false);
});
test("matching text shows requested seats, not fabricated vehicle availability", () => {
    assert.match(requestBody(2, 1.3333), /request for 2 seat/);
    assert.match(requestBody(2, 1.3333), /1.33 kg/);
});
