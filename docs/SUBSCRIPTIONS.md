# PRO accuracy — subscription roadmap

This version **does not charge money** and its $1/month screen is a clearly
labeled *satirical paywall preview*. Debug builds expose an accuracy-preview
button for developer verification. Release builds do not expose that button.

## Real subscription product — not yet activated
- Planned monthly SKU: `accuracy_monthly`; $1 USD/month target.
- Create the subscription and monthly auto-renewing base plan in Google Play Console.
- Display the localized, actual price and renewal terms retrieved from Play Billing,
  never hardcode the real checkout price or promise $1 in every currency.
- Integrate Google Play Billing Library (current documented version 9.1.0 or newer).
- Purchase flow must show exact costs, period and automatic renewal clearly.
- Verify purchases/server subscription status securely; handle cancellations, renewals,
  grace periods, account hold and restores. Never trust a local boolean as entitlement.
- Acknowledge valid new purchases within the Google Play deadline.
- Provide a subscription management/cancellation link.
- Make a test purchase with licensed testers before enabling real purchases.

**Google Play notes:** A subscription must provide sustained/recurring value
and must not mislead users. This is a parody app, so product listings and in-app
disclosures must visibly disclose the intentionally wrong FREE results.

References:
- https://developer.android.com/google/play/billing/integrate
- https://developer.android.com/google/play/billing/subscriptions
- https://support.google.com/googleplay/android-developer/answer/9900533

## Release and GitHub APKs
A publicly downloaded APK from GitHub **does not automatically have working
Google Play subscriptions**. Production billing generally depends on the
configured Play Console app/product and distribution/testing environment.
Do not attempt to charge through an invented paywall or an untrusted link.
