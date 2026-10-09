# Wrongulator development contract

This repository contains one Kotlin/Compose Android joke calculator.
Follow the reasoning standards from https://github.com/kabasik007/Appbootstrap.

- Make the **FREE** result intentionally incorrect and visibly labeled as parody.
- Calculate with BigDecimal correctly in authorized PRO mode; no random arithmetic bugs.
- Never implement hidden billing, surprise charges, fake payment confirmation,
  indefinite paywall loops or unsupported entitlement switches.
- Do not add INTERNET permission, tracking, databases, image libraries or DI frameworks
  without a real product requirement.
- Keep math/state transitions pure Kotlin and test normal, invalid and boundary cases.
- One source of truth for screen state, Compose renders it, buttons dispatch events.
- UI must be accessible, support Ukrainian and English, and adapt to smaller phones.
- Prefer small commits and bounded app footprint, avoid unnecessary abstractions.
- For release: require valid keystore and independent Play Billing/entitlement review.
- Report actual tests and build results; never claim success without evidence.

- The secondary Smart Tools page **must always calculate accurately**, with independently tested pure Kotlin functions; it is free, offline and has no premium entitlement dependency.
- The intentionally inaccurate primary calculator uses a compact FREE/not accurate label; the About page contains an unambiguous satire warning.
