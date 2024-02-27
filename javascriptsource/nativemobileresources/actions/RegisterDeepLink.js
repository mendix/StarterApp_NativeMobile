import { Linking } from 'react-native';

// This file was generated by Mendix Studio Pro.
// BEGIN EXTRA CODE
// END EXTRA CODE
/**
 * This JavaScript actions registers a callback nanoflow which is called very time the app is opened with an URL. The Callback URL Handers nanoflow will receive the URL (type string) as input parameter.
 *
 * Please note that the name of the input parameter is case sensitive and should all upper cases.
 *
 * This requires a custom build of the Mendix Native Shell app to register a schema and host to the OS. See read me.  Please note that this will not work on the Make it Native app.
 * @param {Nanoflow} urlHandler
 * @returns {Promise.<void>}
 */
async function RegisterDeepLink(urlHandler) {
    // BEGIN USER CODE
    const url = await Linking.getInitialURL();
    if (url) {
        await urlHandler({ URL: url });
    }
    Linking.addEventListener("url", async (openObject) => {
        const url = openObject.url;
        try {
            await urlHandler({ URL: url });
        }
        catch (error) {
            console.error("Failed handle deeplink call", url, error);
        }
    });
    // END USER CODE
}

export { RegisterDeepLink };