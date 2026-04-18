# User-Facing Order Return Flow — Design Spec

**Date:** 2026-04-17
**Scope:** User-side fixes to `OrderDetailPage` and `OrderListPage` — status badges, return request button, return modal.

---

## Goal

Fix three gaps in the user-facing order UI:
1. Status badges only handle PAID/PENDING/CANCELLED — missing PROCESSING, SHIPPED, DELIVERED, CLOSED
2. No way for users to submit a return request, even though the backend endpoint exists
3. `orders.js` API module is missing `createReturn()`

---

## Files Changed

| File | Change |
|---|---|
| `frontend/src/api/orders.js` | Add `createReturn()` |
| `frontend/src/components/OrderDetailPage/OrderDetailPage.jsx` | Fix badges, add return button + modal |
| `frontend/src/components/OrderDetailPage/OrderDetailPage.css` | Badge colors for all statuses, modal styles |
| `frontend/src/components/OrderListPage/OrderListPage.jsx` | Fix status badge colors |
| `frontend/src/components/OrderListPage/OrderListPage.css` | Badge colors for all statuses |

---

## Section 1: API — `orders.js`

Add one export:

```js
export function createReturn(token, orderId, body) {
    return fetch(`/api/orders/${orderId}/returns`, {
        method: 'POST',
        headers: authHeaders(token),
        body: JSON.stringify(body),
    }).then(handleResponse)
}
```

`body` shape: `{ reason: string, items: [{ orderItemId: number, quantity: number }] }`

---

## Section 2: Status Badges

Replace the existing `statusClass()` helper in both `OrderDetailPage.jsx` and `OrderListPage.jsx` with one that covers all 7 statuses:

| Status | Badge color |
|---|---|
| PENDING | grey |
| PAID | blue |
| PROCESSING | orange |
| SHIPPED | indigo/purple |
| DELIVERED | green |
| CLOSED | dark grey |
| CANCELLED | red |

Add corresponding CSS classes to both CSS files (e.g. `--pending`, `--paid`, `--processing`, `--shipped`, `--delivered`, `--closed`, `--cancelled`).

---

## Section 3: Return Button and Modal

### 3.1 Return Button

In `OrderDetailPage` header, next to the status badge:

- Show `↩ Return` button **only when** `order.status === 'DELIVERED'` AND `returnSubmitted === false`
- When `returnSubmitted === true`, show a "Return Requested" grey badge in place of the button
- `returnSubmitted` is local component state (no re-fetch needed)

### 3.2 Return Modal

A modal overlay triggered by clicking `↩ Return`.

**Fields:**
- **Reason** — `<textarea>` required, placeholder "Describe why you're returning…"
- **Items to return** — one row per order item:
  - Checkbox to include/exclude the item
  - Quantity `<input type="number">` — default = ordered qty, min = 1, max = ordered qty
  - Quantity input is disabled when checkbox is unchecked

**Validation (client-side, on submit):**
- Reason must not be empty
- At least one item must be checked
- All checked items must have quantity ≥ 1

**Submission:**
- Calls `createReturn(token, orderId, { reason, items })` where `items` contains only the checked rows
- Loading state on submit button ("Submitting…", disabled)
- On success: modal closes, `returnSubmitted` set to `true` → button replaced by "Return Requested" badge
- On error: show error message inside modal, keep modal open

**Dismissal:**
- Cancel button closes modal without submitting
- Does not reset form state (user can reopen and continue)

### 3.3 Modal Styles

All styles in `OrderDetailPage.css`:
- Overlay: fixed full-screen semi-transparent dark background
- Dialog: white card, centered, max-width ~480px, scrollable if tall
- "Return Requested" badge: grey, same pill shape as status badges

---

## Out of Scope

- Showing existing return status fetched from server (the badge is purely local state)
- Allowing multiple return requests per order (backend handles this via conflict check)
- User-facing return history page
