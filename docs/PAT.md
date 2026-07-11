# Creating a GitHub Personal Access Token (PAT) for this app

To sync course content with your GitHub repositories, this app needs
permission to read and write files in those repositories *as you*. You grant
that permission by creating a **fine-grained personal access token (PAT)** on
GitHub and pasting it into the app once.

A fine-grained PAT is much safer than an old-style ("classic") token:

- it works **only on the specific repositories you select** — not everything
  your account can reach;
- it can be limited to **only the permissions the app actually needs**
  (reading and writing repository file contents — nothing else);
- it **expires automatically** on a date you choose.

The app will only accept fine-grained tokens (they start with `github_pat_`).

## What the app does (and doesn't do) with your token

- The token is **encrypted before it is stored**; the database never contains
  the plain token.
- The token is **write-only**: no page or API in this app will ever display it
  back, even to you. The app shows only the last four characters and the
  expiration date so you can recognize which token you entered.
- If you lose the token, nothing is wrong — just create a new one on GitHub
  and paste it into the app again. Entering a new token replaces the old one.

## Step-by-step: creating the token

1. Sign in to GitHub, and go to **Settings** (click your profile photo in the
   upper right corner, then *Settings*).
2. In the left sidebar, scroll to the bottom and click **Developer settings**.
3. Click **Personal access tokens → Fine-grained tokens**, then
   **Generate new token**.
4. Fill in the form:
   - **Token name**: something you'll recognize later, e.g.
     `scaffold-app-sync`.
   - **Expiration**: pick a date. Shorter is safer; you'll paste in a new
     token when it expires. (The app will show you the expiration date you
     record so you know when a replacement is due.)
   - **Resource owner**: choose the account or organization that owns the
     repositories the app should access. If the repositories belong to an
     organization, select that organization here — *not* your personal
     account.
   - **Repository access**: choose **Only select repositories**, then select
     exactly the repositories the app should sync. Do **not** choose "All
     repositories".
   - **Permissions → Repository permissions**: set **Contents** to
     **Read and write**. GitHub will automatically add **Metadata:
     Read-only**; that's expected. Leave every other permission at
     **No access**.
5. Click **Generate token**, and copy the token. It starts with
   `github_pat_`. **This is the only time GitHub will show it to you.**

> **If the repositories belong to an organization:** the organization may
> require an owner to approve fine-grained tokens. In that case your token
> shows as "pending approval" until an org owner approves it — a one-time
> approval that covers all the repositories you selected. If the app's sync
> doesn't work right away, check the token's status on the same GitHub page
> where you created it.

## Entering the token in the app

Paste the token into the app right away (you can't view it on GitHub again
later). Enter it together with the expiration date you chose, so the app can
warn you before it lapses.

Currently the token is entered via the API endpoint `POST /api/pat` (for
example, through the Swagger UI at `/swagger-ui/index.html`, under
**PatCredential**), with:

- `token` — the `github_pat_...` value you copied;
- `expiresAt` — the expiration date you picked, in `YYYY-MM-DD` format
  (optional but recommended).

You can check what the app has on file for you with `GET /api/pat` — it
returns only the last four characters and the expiration date.

## Replacing a token

Whenever your token expires, is revoked, or is lost, create a new one using
the same steps and enter it again. The new token replaces the old one; there
is nothing to delete first.
