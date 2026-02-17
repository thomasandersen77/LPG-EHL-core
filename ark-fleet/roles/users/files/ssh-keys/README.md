# SSH Public Keys

Place SSH public keys for users in this directory.

## Single User Keys

Each file should be named `<username>.pub` and contain the user's SSH public key.

For example:
- `alejandro.pub` - SSH public key for alejandro user
- `thomas.pub` - SSH public key for thomas user

These keys will be deployed to `/home/<username>/.ssh/authorized_keys` on the target systems.

## Shared Account with Multiple Keys

For the `arkadmin` shared account, multiple developers can access using their individual keys:

- `arkadmin-dev1.pub` - First developer's SSH public key
- `arkadmin-dev2.pub` - Second developer's SSH public key

Both keys will be added to `/home/arkadmin/.ssh/authorized_keys`, allowing either developer to SSH as `arkadmin`.

### Adding Your Key for arkadmin

```bash
# Copy your SSH public key
cp ~/.ssh/id_rsa.pub roles/users/files/ssh-keys/arkadmin-dev1.pub

# Or for the second developer
cp ~/.ssh/id_rsa.pub roles/users/files/ssh-keys/arkadmin-dev2.pub
```

## Security Note

Only store **public** keys here. Never commit private keys to version control.

## Connecting

After deployment:

```bash
# Connect as individual user
ssh alejandro@ark-device-ip

# Connect as shared admin user
ssh arkadmin@ark-device-ip
```

Both individual users and shared account users will have sudo access.
