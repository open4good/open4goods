import {
  resolveActiveEventPack,
  eventPackSchedule,
} from "./frontend/config/theme/event-packs";

console.log("--- Verification Script ---");
console.log("Testing date: 2025-12-24");

// Mock date by overriding Date constructor or just passing it if the function supports it.
// The function signature is: resolveActiveEventPack(date: Date = new Date())
const testDate = new Date("2025-12-24T12:00:00Z");
const pack = resolveActiveEventPack(testDate);

console.log(`Resolved Pack: ${pack}`);

// Check specific scheduled items order
const christmasIndex = eventPackSchedule.findIndex(
  (p) => p.id === "christmas-december"
);
const winterIndex = eventPackSchedule.findIndex(
  (p) => p.id === "winter-highlights"
);

console.log(`Christmas Index: ${christmasIndex}`);
console.log(`Winter Index: ${winterIndex}`);

if (pack === "christmas" && christmasIndex < winterIndex) {
  console.log(
    "✅ SUCCESS: Christmas pack is active and correctly prioritized."
  );
  process.exit(0);
} else {
  console.error("❌ FAILURE: Christmas pack is NOT active or NOT prioritized.");
  console.log("Expected: christmas");
  console.log(`Actual: ${pack}`);
  process.exit(1);
}
