## What and why

<!-- What does this change, and which problem does it solve? Link the issue: Closes #… -->

## SAT sources

<!-- For rules, formulas or catalogs: document, version and page. Write "n/a" otherwise. -->

## Checklist

- [ ] `./mvnw -B clean verify` passes locally.
- [ ] New or changed rules have a valid and an invalid case, and the coverage reports are regenerated.
- [ ] Only SAT public test data is used. There are no real RFCs, certificates, keys or personal data.
- [ ] Generated files were changed only through the generator.
- [ ] `CHANGELOG.md` is updated under *Unreleased* if users will notice the change.
- [ ] Commits follow Conventional Commits.
