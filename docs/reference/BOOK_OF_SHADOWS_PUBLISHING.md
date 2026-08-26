# Book of Shadows Publishing

The canonical standalone guide is the repository-root
`Book_of_Shadows.html`. Do not edit the public mirror independently.

Published locations:

- Live guide: `https://thorelvin.github.io/witchery-book-of-shadows/`
- Public mirror repository: `https://github.com/thorelvin/witchery-book-of-shadows`
- Canonical source: `Book_of_Shadows.html` in this repository

The mod repository is private and its current GitHub plan does not support
Pages for private repositories. The separate public repository therefore
contains only the standalone guide, a short README, and its Pages workflow.

After a canonical book change:

1. Copy `Book_of_Shadows.html` unchanged to both `index.html` and
   `Book_of_Shadows.html` in the public mirror checkout.
2. Verify that both published files have the same SHA-256 hash as the canonical
   source and that the HTML still contains a complete document shell.
3. Commit and push the public mirror's `main` branch as `thorelvin`.
4. Wait for the `Publish Book of Shadows` workflow and confirm the live page
   returns successfully.

The mirror is intentionally manual: the private repository does not carry a
cross-repository deployment credential, and the public repository must never
become an alternate source of truth.
