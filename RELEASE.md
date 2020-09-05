# Releasing

- Bump version numbers in `package.json` and `plugin.xml`
- `git commit -a` to commit outstanding changes
- `git tag -s x.y.z -m 'Release x.y.z.'` to create release tag
- `git push && git push --tags` to push changes
- `npm publish` to publish NPM package
