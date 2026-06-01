# ChatApp — UI/UX & Visual Design Improvement Plan

> A senior product-designer review of the existing **Java Swing** chat client, with a
> concrete, Swing-native modernization strategy. No code changes are proposed here —
> this is the design brief that a future implementation pass would follow.

---

## 0. Context: what we are actually working with

This is **not** a web app. There is no CSS, no flexbox, no DOM. The stack is:

- **Java Swing** with the default (Metal/system) Look-and-Feel.
- Screens: `LoginFrame`, `RegisterFrame`, `MainFrame` (header + left `JSplitPane` with two
  `JList`s + right `JTabbedPane`), per-conversation `ChatPanel` / `GroupPanel`, plus
  voice/video frames.
- Conversations render as a **single linear `JTextPane` log** — every message is one
  appended line: `[HH:mm] who: text`. There are **no message bubbles, no left/right
  alignment, no avatars** today.
- Online users come from a server `USER_LIST` broadcast into a `DefaultListModel<String>`;
  unread counts are shown inline as `name  (3)` via a `DefaultListCellRenderer`.
- Vietnamese copy throughout ("Đang online", "Gửi", "Đăng xuất"). **Keep the language**;
  modernize the presentation.

**The single most important architectural decision:** the linear-text-log conversation view
is the root cause of why the app looks dated. Every high-impact visual goal in the request
(left/right bubbles, avatars in-thread, read receipts, typing indicators, shadows) is
**impossible inside one `JTextPane`** and trivial once each message is its own component.
So the spine of this plan is: **replace the `JTextPane` log with a vertically-stacked panel
of message-bubble components.** Everything else hangs off that.

---

## 1. Complete UI/UX Enhancement Strategy

### 1.1 Adopt a modern Look-and-Feel as the foundation
Before touching individual screens, swap the L&F. **FlatLaf** (com.formdev.flatlaf) is the
industry-standard modern Swing L&F — it gives you, for free and globally:

- Clean flat components, proper hover/pressed states, rounded controls.
- A **light and dark theme** (FlatLightLaf / FlatDarkLaf) you can toggle.
- Centralized theming via UI properties (`Component.arc`, `Button.arc`,
  `TextComponent.arc`, accent color) so the whole app re-skins from a handful of values.
- Native-feeling focus rings and selection colors — a big accessibility win at zero
  per-component cost.

This one change moves the app ~60% of the way to "modern" before any custom painting.
Set it once in `main()` before the first frame is created.

### 1.2 Establish a design-token layer
Create a single `Theme`/`UI` constants holder (colors, radii, spacing scale, fonts,
durations). Today colors are hardcoded ad hoc (`new Color(110,110,110)`,
`new Color(20,90,200)`). Centralizing them is the Swing equivalent of CSS variables and is
the precondition for a consistent, switchable palette (§4).

### 1.3 Re-architect the conversation view into components
Replace the per-conversation `JTextPane history` with:

- A scrollable **`messagesPanel`** using a vertical `BoxLayout` (or MigLayout) inside a
  `JScrollPane`.
- One **`MessageBubble` component per message**, which knows its `sender`, `isMine`,
  `timestamp`, `status`, and content. It paints its own rounded background + shadow in
  `paintComponent` and aligns left/right via an enclosing row panel with a horizontal glue.
- A lightweight **day-divider** component ("Hôm nay", "Hôm qua", date) inserted when the
  date changes.
- Keep file links and emoji-font-switching behavior, but move them inside the bubble.

This is the keystone. It unlocks bubbles, alignment, avatars, per-message status, hover
actions, and grouping.

### 1.4 Treat the left rail as a real "conversation list", not two raw `JList`s
The current sidebar shows online users and groups as plain text rows. Modernize both into
**rich rows** rendered by custom `ListCellRenderer`s: avatar + name + last-message preview +
timestamp + unread badge + presence dot. (Details in §3.)

### 1.5 Introduce state-driven UI: empty, loading, typing
The app currently has exactly one empty state ("Chọn người dùng bên trái để bắt đầu chat.")
and no loading or typing states. Add a small set of reusable state panels (§3.9, §5).

### 1.6 Keep behavior identical, change only presentation
No protocol/message-type changes are required for the *visual* work. Typing indicators and
read receipts ideally need new signals, but can be **simulated/staged first** (see §3.6/§3.7
"phased" notes) so the UI can ship before the wire protocol catches up.

---

## 2. Specific Visual Design Recommendations

### 2.1 Spacing & layout system
Adopt an **8px spacing scale** (4 / 8 / 12 / 16 / 24 / 32). Replace the scattered
`EmptyBorder(6,8,4,8)` / `FlowLayout(…,4,0)` values with tokens from this scale. Consistent
rhythm is the cheapest, highest-return visual upgrade after the L&F swap.

- Conversation rows: 12px vertical padding, 12–16px horizontal.
- Message bubbles: 8–10px vertical / 12–14px horizontal interior padding; 2–3px gap between
  consecutive bubbles from the same sender, 12px gap when the sender changes.
- Window content gutters: 16px.

### 2.2 Corner radius
- Buttons / inputs / cards: **8px** (`Button.arc`, `Component.arc` in FlatLaf).
- Message bubbles: **16px**, with the "tail" corner reduced to ~4px (the corner nearest the
  sender) — the classic Messenger/iMessage asymmetric bubble.
- Avatars: full circle.

### 2.3 Elevation & shadows
Swing has no box-shadow, so paint it: in `paintComponent`, draw the rounded rect, then a
soft 1–2px translucent dark border or a faux drop-shadow (offset translucent rounded rect
underneath). Use **sparingly**:
- Bubbles: a very subtle 1px shadow / 8% black border for separation from the background.
- Floating elements (emoji popup, message hover-actions, send button): slightly stronger.
- Sidebar and header: prefer a **1px divider line** over a shadow.

### 2.4 Typography
Standardize on **Segoe UI** (already used) with a clear type ramp, and **Segoe UI Emoji**
for emoji codepoints (the existing per-codepoint font switch is good — preserve it):

| Role | Size / weight |
|---|---|
| Window/title | 16–18 Bold |
| Section header ("Đang online") | 12 Semibold, letter-spaced, muted |
| Conversation name | 14 Semibold |
| Message body | 14 Regular (bump from 13) |
| Last-message preview / metadata | 12 Regular, muted |
| Timestamp | 11 Regular, muted |

Increase body text from 13→14 and **increase line spacing** in bubbles (StyleConstants line
spacing or component insets) for readability.

### 2.5 Iconography
Replace emoji-as-icons (📎 🎙 📹 😀 ✕) with a real monochrome icon set (e.g. FlatLaf's
extras / Feather / Material icons as SVG via `FlatSVGIcon`). Emoji glyphs render
inconsistently across machines and read as "unfinished". Icons should be 16–20px, single
color, and tint on hover.

### 2.6 Visual hierarchy in the chat window
Today every message line is visually identical weight. Establish hierarchy:
- **Sender name**: only shown on the *first* bubble of a group (and only for the *other*
  party / in groups), 12 Semibold, accent-tinted in group chats for per-user colors.
- **Body**: primary weight/contrast.
- **Time + status**: smallest, lowest contrast, bottom-right inside or beneath the bubble.

---

## 3. Component-by-Component Improvements

### 3.1 Login & Register (`LoginFrame`, `RegisterFrame`)
First impression — currently a bare `GridBagLayout` form.
- Center a **card** (rounded panel, subtle shadow, ~360px wide) on a soft background
  (flat tint or a gentle vertical gradient painted in the content pane).
- App **logo/wordmark** at top; tagline beneath.
- Inputs: full-width, 40px tall, 8px radius, **leading icons** (user / lock / server),
  floating or fixed labels, visible focus ring.
- Password field: show/hide toggle (eye icon).
- Primary button (Đăng nhập) full-width, accent-filled; secondary (Đăng ký) as a quiet
  text/link button. Server picker + "Quản lý…" grouped as a compact row with a small
  connection-status dot.
- **Inline validation** under fields instead of `JOptionPane` error dialogs (keep dialogs
  only for connection failures). Show a spinner + "Đang kết nối…" on the button during login.

### 3.2 Main window shell (`MainFrame`)
- **Header**: slim app bar — avatar of current user (with presence dot) + name on the left,
  a search field in the center (filter conversations), and an overflow/settings menu +
  "Đăng xuất" on the right. Replace the bold text button with an icon + label, quieter.
- Convert the **left `JSplitPane`** into a single unified left rail with two collapsible,
  labeled sections ("TRỰC TUYẾN" / "NHÓM") rather than a draggable split — or keep the split
  but restyle dividers to 1px hairlines and remove the one-touch arrows' heaviness.
- Give the rail a **distinct surface color** from the conversation area (the standard
  two-tone messaging layout: slightly darker/tinted sidebar, lighter thread).
- Rail width ~280px (current 240 is tight for rich rows).

### 3.3 Online-users list → presence-aware conversation rows
Custom `ListCellRenderer` per row:
- **Avatar** (40px circle) with fallback **initials** on a deterministic color derived from
  the username hash (so each person gets a stable color). (§3.8)
- **Green presence dot** (10px, white ring) overlapping the bottom-right of the avatar —
  this directly satisfies the "green online indicator" requirement. Since `USER_LIST` *is*
  the set of online users, everyone in this list is online → green; reserve grey/idle for a
  future presence signal.
- **Name** (14 Semibold) on top, **last-message preview** (12 muted, single line, ellipsized)
  beneath. Bold the name + preview when there are unreads.
- **Right column**: last-activity time (11 muted) on top; **unread badge** beneath — a
  filled accent pill with the count (replaces today's `name (3)` text). Cap at "9+".
- **Hover**: subtle background tint; **selected**: stronger accent-tinted background with a
  2–3px accent bar on the leading edge.
- Row height ~64px, comfortable 12px padding.

### 3.4 Groups list (`GroupPanel` entries)
Same rich-row renderer, but:
- Group **avatar**: a stacked/“people” glyph or a 2×2 montage of member initials.
- Show member count and the last sender ("Minh: …") in the preview line.
- Keep the 👥 affordance but as a proper icon, not an emoji in the tab title.

### 3.5 Conversation / chat window (`ChatPanel`) — the centerpiece
This is where the redesign earns its keep.
- **Thread header**: avatar + name + presence/"đang hoạt động" subtitle on the left; call
  buttons (voice 🎙→icon, video 📹→icon) and an overflow menu on the right. Replace the bare
  "Chat với X" label.
- **Message area**: stacked **bubbles** (see §1.3):
  - **My messages: right-aligned**, accent-colored fill, white/high-contrast text.
  - **Their messages: left-aligned**, neutral surface fill (light grey in light theme,
    elevated grey in dark), with the sender's avatar to the left of the *first* bubble in a
    run (omit avatar on continuation bubbles for a clean gutter).
  - **Bubble internals**: body text, then a small footer row with **timestamp** and, for my
    messages, a **status tick** (§3.7). Long-press/hover reveals full timestamp tooltip.
  - **Grouping**: collapse consecutive same-sender messages within ~5 min — show name/avatar
    once, tight 2px gaps; insert **day dividers** on date change.
  - **File messages**: render as a distinct attachment card inside the bubble (file icon +
    name + size + "mở" action), not an inline underlined link.
  - **Auto-scroll** to bottom on new message **only if** already near the bottom; otherwise
    show a floating "↓ tin nhắn mới" pill that jumps to bottom on click.
- **Input area** (§3.10).

### 3.6 Typing indicator
- In-thread: a left-aligned **animated three-dot bubble** (`Timer`-driven dot opacity/bounce)
  shown while the peer is typing; in the rail/header subtitle, show **"X đang nhập…"**.
- **Phasing**: visually build it now and drive it from a local heuristic or a debug toggle;
  wire it to a real `TYPING`/`STOP_TYPING` signal when the protocol gains one. Emit on
  keystroke with a 2–3s debounce; clear on send.

### 3.7 Delivery / read status
- Per *my* message, a tick affordance in the bubble footer: **sending** (clock) → **sent**
  (single check) → **delivered** (double check) → **read** (double check, accent-colored) —
  the WhatsApp/Telegram convention.
- **Phasing**: today there is no ACK-per-message channel. Start with **sent** (single check
  immediately on local append) and **delivered** when the server echoes; "read" needs a read
  receipt signal — show the placeholder state until then. Keep the visual language ready.

### 3.8 Avatars (cross-cutting)
- Single reusable `Avatar` renderer used in rail, thread header, and bubbles.
- **Consistent sizes**: 40px (rail), 32px (thread header), 28px (in-thread, other party).
- **Fallback initials**: 1–2 letters from the username, centered, white on a deterministic
  HSB color seeded by the username hash → stable, distinct per-user colors that also serve as
  per-sender name colors in group chats.
- Circular clip; 1px subtle ring for separation on busy backgrounds.

### 3.9 Empty, loading & error states
- **No conversation selected**: friendly illustration/glyph + "Chọn một cuộc trò chuyện để
  bắt đầu" + hint. (Upgrade the current plain label.)
- **No messages yet in a thread**: centered avatar + "Hãy gửi lời chào tới X 👋".
- **No groups**: "Bạn chưa có nhóm nào" + prominent "Tạo nhóm" CTA.
- **No search results**: "Không tìm thấy" state.
- **Loading**: skeleton loaders (§5.4) for both the rail and the thread on first open.
- **Disconnected**: a slim banner under the header ("Mất kết nối — đang thử lại…") rather
  than only error dialogs.

### 3.10 Message input
- A single **rounded container** (pill or 12px-radius card) holding: emoji button (left),
  the multiline text area (auto-growing 1→~5 lines), attach/file button, and a **circular
  accent send button** with a paper-plane icon on the right.
- **Focus state**: container border brightens to accent + subtle glow; placeholder text
  "Nhắn tin tới X…".
- Keep the existing Enter/Shift+Enter/Ctrl+Enter logic and the "ENTER gửi" preference, but
  move the hint into a tooltip/quiet helper to reduce toolbar clutter.
- Send button **disabled/greyed when input is empty**, accent + subtle scale on press.

### 3.11 Chat tabs (`JTabbedPane`)
- Restyle tabs flat (FlatLaf) with rounded active indicator; the close "✕" becomes a hover-
  revealed icon. Show a small unread dot on background tabs.
- Consider that tabs + a conversation list is somewhat redundant; long-term, selecting a rail
  row could drive a **single thread pane** (master–detail) instead of spawning tabs. Out of
  scope for a pure visual pass, but note it as a structural simplification.

### 3.12 Dialogs (`CreateGroupDialog`, `ServerManagerDialog`, incoming call)
- Re-skin to match: card padding, 8px controls, primary/secondary button hierarchy, icons.
- **Incoming call**: large caller avatar, pulsing ring animation, clearly separated
  red "Từ chối" / green "Trả lời" buttons.

---

## 4. Modern Color Palette

Define once as tokens; provide **light and dark** values. Accent is a calm indigo/blue
(trustworthy, distinct from typical green-only messengers, gives ChatApp its own identity).

### Light theme
| Token | Hex | Use |
|---|---|---|
| `accent` | `#4F46E5` | my bubbles, primary buttons, active states |
| `accent-hover` | `#4338CA` | pressed/hover |
| `accent-soft` | `#EEF2FF` | selected row tint, focus glow |
| `bg-app` | `#FFFFFF` | thread background |
| `bg-sidebar` | `#F7F8FA` | left rail surface |
| `bg-bubble-other` | `#F1F3F5` | incoming bubbles |
| `surface-card` | `#FFFFFF` | cards, input, dialogs |
| `border` | `#E5E7EB` | hairlines, dividers |
| `text-primary` | `#111827` | body |
| `text-secondary` | `#6B7280` | previews, metadata |
| `text-on-accent` | `#FFFFFF` | text on my bubbles/buttons |
| `online` | `#22C55E` | presence dot |
| `unread` | `#EF4444` or `accent` | unread badge |
| `danger` | `#EF4444` | logout, reject call, destructive |

### Dark theme
| Token | Hex |
|---|---|
| `accent` | `#6366F1` |
| `bg-app` | `#0F1117` |
| `bg-sidebar` | `#161922` |
| `bg-bubble-other` | `#222634` |
| `surface-card` | `#1B1F2A` |
| `border` | `#2A2F3C` |
| `text-primary` | `#E5E7EB` |
| `text-secondary` | `#9CA3AF` |
| `online` | `#22C55E` |

**Contrast:** verify all text/background pairs meet **WCAG AA (4.5:1 body, 3:1 large)**.
White-on-`#4F46E5` and the chosen greys all clear AA. Never rely on color alone for status —
pair the green dot with a tooltip/"Đang hoạt động", and ticks with shape changes, not just
color, for read state.

### Tasteful "modern" finishes (use restraint)
- **Gradients**: only on the login background and optionally a faint accent gradient on my
  bubbles. Not on every surface.
- **Glassmorphism**: reserve for transient overlays (emoji popup, hover action bar, incoming-
  call sheet) — a translucent blur-ish panel. Swing can't truly blur cheaply, so simulate
  with a semi-transparent fill + 1px light border. Don't glass the main surfaces (hurts
  contrast).
- **Soft shadows**: bubbles and floating menus only (§2.3).

---

## 5. Animation & Micro-interactions

All Swing animation = `javax.swing.Timer` tweening a value used in `paintComponent`, or the
**FlatLaf animation** utilities. Keep durations **120–220ms**, ease-out. Respect a global
"reduce motion" toggle.

1. **Message send**: new bubble fades + slides up ~8px / scales 0.97→1 on append. Send button
   does a quick press-scale and the paper-plane nudges.
2. **Incoming message**: gentle fade-in; if scrolled up, the "↓ tin nhắn mới" pill slides in.
3. **Typing dots**: looping 3-dot bounce/opacity (§3.6).
4. **Conversation switch**: cross-fade or 100ms slide of the thread pane; selected rail row
   animates its accent bar in.
5. **Hover**: rail rows, buttons, bubbles' hover-action bar fade their background tint
   (don't snap).
6. **Unread badge**: subtle pop-scale when the count increments.
7. **Presence**: when a user comes online, their dot fades grey→green; row can briefly flash
   accent-soft.
8. **Skeleton shimmer**: a moving gradient highlight across skeleton blocks while loading.
9. **Focus**: input border/glow eases in on focus.
10. **Button press**: 0.96 scale + slightly darker fill, springs back.
11. **Tab close / open**: fade rather than instant add/remove.
12. **Call ring**: pulsing concentric rings around the caller avatar in the incoming-call
    dialog.

**Skeleton loaders (5.4):** rounded grey placeholder rows (avatar circle + 2 text bars) for
the rail, and alternating left/right placeholder bubbles for the thread, with a shimmer
sweep, shown until the first real data arrives.

---

## 6. Responsive Design

Swing isn't fluid by default, but a desktop chat app still needs to behave at multiple sizes.
Drive layout off the frame width with `ComponentListener` breakpoints:

- **Compact (< ~720px / "mobile-like")**: single pane. Show **either** the conversation list
  **or** the open thread, with a back arrow in the thread header to return to the list. Hide
  the search field behind an icon. Collapse the input toolbar into an overflow "+".
- **Medium / tablet (~720–1000px)**: rail (narrow, ~240px) + thread. Avatars stay; previews
  may truncate harder. Rail can become **icon-only collapsible** via a hamburger toggle.
- **Wide / desktop (> ~1000px)**: full rail (280px) + thread + room for an optional right
  "details" panel (group members, shared files).
- Make the rail width **draggable but clamped** (min 240, max 360); persist the user's choice.
- Bubbles use a **max width of ~72%** of the thread width so long lines wrap naturally instead
  of stretching edge to edge.
- Set sensible **minimum frame size** so the layout never collapses into unusable slivers.
- Ensure all `JScrollPane`s show smooth, thin, modern scrollbars (FlatLaf does this) and that
  the thread keeps its scroll position correctly on resize.

---

## 7. Accessibility

- **Contrast**: enforce AA across the palette (§4); audit the muted greys especially.
- **Focus indicators**: visible 2px accent focus ring on every interactive control
  (FlatLaf provides this; don't suppress it). Never remove focus painting for "cleanliness".
- **Keyboard navigation**:
  - Tab order through rail → thread → input that makes sense.
  - Arrow-key navigation within the conversation list; **Enter** opens the selected
    conversation (today it's double-click only — add keyboard open).
  - Global shortcuts: Ctrl+K to focus search/jump-to-conversation, Ctrl+Tab to cycle tabs,
    Esc to close a thread/dialog.
  - Keep and document the Enter/Shift+Enter/Ctrl+Enter send behavior in a tooltip.
- **Screen-reader names**: set `getAccessibleContext().setAccessibleName/Description` on
  avatars (the person's name), unread badges ("3 tin chưa đọc"), status ticks ("đã xem"),
  presence dots ("đang hoạt động"), and icon-only buttons. Icon buttons must always have
  tooltips + accessible names.
- **Don't encode meaning in color alone**: presence = dot **+** label/tooltip; read state =
  tick **shape** change, not just color; unread = badge **+** bold text.
- **Hit targets**: ≥ 32–40px for buttons, badges, close affordances.
- **Text scaling**: read sizes from the `Theme` token layer so a future "larger text" option
  scales everything consistently.
- **Reduced motion**: a setting that disables the §5 animations.

---

## 8. Prioritized Roadmap (highest impact → lowest)

### Tier 1 — Foundational, do first (transforms the whole app cheaply)
1. **Integrate FlatLaf** (light+dark) and set global arc/accent UI properties. *(Single
   biggest visual jump for least effort.)*
2. **Create the `Theme` token layer** (colors, spacing, radii, fonts, durations).
3. **Rich conversation-list rows** via custom `ListCellRenderer`: avatar + initials,
   **green presence dot**, name, last-message preview, time, **unread badge pill**, hover +
   selected states. *(Directly satisfies the green-indicator + list-redesign asks.)*

### Tier 2 — The centerpiece (defines "modern messaging app")
4. **Replace the `JTextPane` log with component bubbles**: left/right alignment, my-vs-theirs
   colors, rounded asymmetric corners, padding, timestamp footer, soft shadow.
5. **Message grouping + day dividers + avatars in-thread.**
6. **Redesigned input area**: rounded container, focus state, circular paper-plane send
   button, empty-disabled state.
7. **Restyled thread header** with avatar, presence subtitle, icon call buttons.

### Tier 3 — Polish & states
8. **Empty states** (no conversation / no messages / no groups / no results).
9. **Skeleton loaders + loading states** for rail and thread.
10. **Login/Register card redesign** with inline validation and connection feedback.
11. **Real icon set** replacing emoji-as-icons; restyled tabs and dialogs.

### Tier 4 — Motion & advanced signals
12. **Micro-interactions** (§5): send/receive animation, hover tints, button press, focus,
    badge pop, conversation-switch transition.
13. **Typing indicator** (animated dots) — visual first, wire to a `TYPING` signal later.
14. **Delivery/read status ticks** — sent/delivered now, read when a receipt signal exists.

### Tier 5 — Structure & responsiveness
15. **Responsive breakpoints** (compact/tablet/desktop), collapsible rail, master–detail
    consideration vs. tabs.
16. **Accessibility hardening** (keyboard open, shortcuts, accessible names, reduced-motion
    toggle) — ideally folded into each tier as you build, not bolted on last.
17. **Theme switcher** (light/dark/system) in settings.

---

## 9. Notes, risks & dependencies

- **Protocol gaps**: typing indicators and read receipts need new message types
  (`TYPING`, `READ`). The visual layer can ship ahead using staged/placeholder states; flag
  these as "needs backend signal" so they aren't mistaken for fully wired features.
- **Avatars are synthetic**: there are no uploaded profile pictures — initials-on-color is
  the correct, robust default and should remain the fallback even if uploads are added later.
- **Emoji rendering** already correctly switches to *Segoe UI Emoji* per codepoint; preserve
  this logic when moving content into bubbles.
- **EDT discipline**: all the existing `SwingUtilities.invokeLater` usage is correct; new
  animation `Timer`s and bubble updates must also stay on the EDT.
- **Performance**: a component-per-message thread should **recycle/virtualize** or cap
  rendered history (e.g., lazy-load older messages on scroll-up) so very long histories don't
  build thousands of components.
- **Scope discipline**: this plan deliberately changes **presentation only**. It does not
  alter networking, history-file formats, or message routing.

---

### One-paragraph design prompt (for a build pass)

> Re-skin the Swing ChatApp into a modern messaging client without changing its behavior or
> Vietnamese copy. Adopt FlatLaf (light+dark) and a centralized design-token layer
> (indigo `#4F46E5` accent, 8px spacing scale, 8/16px radii, Segoe UI ramp). Rebuild the left
> rail as rich 64px conversation rows — circular initials-avatar with a green online dot,
> name, muted last-message preview, right-aligned time and an accent unread-count pill, with
> hover and accent-bar selected states. Replace the linear `JTextPane` conversation log with a
> vertically stacked panel of self-painting **message-bubble** components: my messages
> right-aligned in accent fill, others left-aligned in neutral grey with the sender's avatar,
> 16px asymmetric corners, soft shadow, generous padding, a timestamp+status-tick footer,
> same-sender grouping and day dividers. Modernize the input into a rounded container with a
> focus glow and a circular paper-plane send button. Add empty states, skeleton loaders,
> an animated typing indicator, delivery/read ticks, and 120–220ms ease-out micro-interactions
> for send/receive/hover/switch. Make it responsive (collapse to single-pane under ~720px) and
> accessible (AA contrast, visible focus rings, keyboard open + shortcuts, accessible names,
> reduced-motion toggle). Aim for Telegram/Messenger-grade polish with its own calm indigo
> identity — modern, not decorative.
