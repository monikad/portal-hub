# Portal Hub Roadmap

## Immediate

- Show read-only Apple Calendar events on the Home dashboard.
- Add read-only Google Calendar events using an `.ics` subscription URL.
- Keep calendar URLs out of Git by loading them from `app/src/main/assets/calendar-feeds.local.json`.
- Rebuild and reinstall the private APK on the Portal.

## Next

- Add a settings screen for calendar feed URLs and backend URL.
- Improve recurring event handling for calendar feeds.
- Show calendar source labels clearly when multiple feeds are active.
- Add a refresh status message for calendar fetch failures.

## Assistant Layer

- Keep the Portal useful without a backend.
- Add optional backend summaries for Home, Execution, Strategy, Career, and Writing.
- Use local models first for summaries and planning.
- Use paid models only for higher-quality writing, strategy, or interview practice.

## Device And Sync

- Keep notes and routines local-first on the Portal.
- Support optional sync through a local backend on a laptop, Raspberry Pi, or another home device.
- Avoid storing private calendar credentials on the Portal.
- Prefer read-only calendar links over full calendar account access.

## Later

- Add push-to-talk voice commands inside the app.
- Explore wake-word support only if it can run locally and reliably.
- Add richer music/routine integrations.
- Improve onboarding docs for people repurposing old Portal devices.
