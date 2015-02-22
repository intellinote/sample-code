Current examples:

 * `lib/oauth-annotated.js` - example of using the OAuth2 protocol to authenticate to the Intellinote API and access user content via the Intellinote REST API.

    * See `example-output/oauth-annotated.color.txt` and `example-output/oauth-annotated.no-color.txt` for sample outputs from that script.

 * `lib/oauth-bare.js` - a stripped-down version of the above, with comments and annotations removed.

To run these examples:

1. Run `npm install` to load the external libraries used by the sample scripts.

2. Copy `config.json.sample` to `config.json`.  Edit that file to insert your `client_id` and `client_secret` values.

3. Run: `node ./lib/oauth-annotated.js <USERNAME> <PASSWORD>`
