# Secrets

This directory can contain secrets e.g. the AUTH0 secret for the ook application.

Integrant profiles that use the `ook.concerns.integrant/secret` reader will first look for an environmental variable, and then fallback to decrypting the value from an equivalently named file, e.g. `secrets/AUTH0_SECRET.gpg`.

You should use gpg to encrypt values for your own private key. The `ook.concerns.integrant/decrypt` function will just call `gpg -d filename.gpg`. Do this with e.g.

```bash
echo VALUE_OF_THE_SECRET | gpg -e -r YOUR_PGP_ID > resources/secrets/AUTH0_SECRET.gpg
```
