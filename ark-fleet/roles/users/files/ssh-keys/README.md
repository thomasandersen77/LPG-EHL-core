# SSH Public Keys

Place SSH public keys for users in this directory.

Each file should be named `<username>.pub` and contain the user's SSH public key.

For example:
- `alejandro.pub` - SSH public key for alejandro user
- `thomas.pub` - SSH public key for thomas user

These keys will be deployed to `/home/<username>/.ssh/authorized_keys` on the target systems.

## Security Note

Only store **public** keys here. Never commit private keys to version control.
