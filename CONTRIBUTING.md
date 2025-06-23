# Contributing to Calva Power Tools

Thanks for considering contributing to Calva Power Tools!

You can contribute in many ways, participating in Discussions, reporting Issues, filing PRs, telling the world about this extension...

## Suggesting Enhancements

The default way of progress for a change to this project is:

1. A [Discussions thread](https://github.com/BetterThanTomorrow/calva-power-tools/discussions) has concluded that there is an issue to be addressed
1. An [Issue](https://github.com/BetterThanTomorrow/calva-power-tools/issues) has been filed, clearly stating the problem
1. A Pull Request is filed addressing the Issue

### Pull Requests

We prefer that each pull request is focused on one problem at the time. Even if sometimes related Issues can be addressed by the same changes.

As part of your PR you update the `**[Unreleased]**` section of CHANGELOG.md, linking to the issue(s) addressed. WHen a new version of the extension is released a changelog entry will be created with everything that is in the unreleased section. The Changelog should read like a story of how the extension has evolved since its creation.

When you file a pull request some CI jobs will run:

1. Checking that the changes lints (using [clj-kondo](https://github.com/clj-kondo/clj-kondo))
1. Checking that the changes are formatted according to [cljfmt](https://github.com/weavejester/cljfmt)
1. Building the extension VSIX
1. Running the unit tests
1. Running the e2e tests using the VSIX

You can also do these tests locally to save yourself time.

## Any questions?

Feel free to reach out if you have any questions.

Thanks again for your contribution!
