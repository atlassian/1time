# 1time
[![Atlassian license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](CONTRIBUTING.md)

Lightweight implementation of RFC-6238 and RFC-4226 to generate and validate time-based one-time passwords (TOTP).

## Quick start

This library is suitable for both prover and verifier. 

### Prover

As described in RFC-6238, the prover is the user trying to authenticate and submitting the generated TOTP to the verifier.

An example is: The user is trying to access an online service and is required to input the TOTP after authenticating with username and password. 
The user will need an application to generate such TOTP.

To generate the current TOTP for a given user's secret, do:

Assuming the user's enrollment has a base 32 encoded secret key: `ZIQL3WHUAGCS5FQQDKP74HZCFT56TJHR`

We can then generate the TOTP for the current time step:

```kotlin
val secret = TOTPSecret.fromBase32EncodedString("ZIQL3WHUAGCS5FQQDKP74HZCFT56TJHR")
val totpGenerator: TOTPGenerator = TOTPGenerator()
val totp = totpGenerator.generateCurrent(secret) //TOTP(value=123456)
```
You can now present the value of `totp` to the user.

### Verifier

As described in RFC-6238, the verifier is the system validating the user's credentials, including multi-factor authentication. After the system validates the username/password, it then verifies the user's TOTP input.

But before being able to verify a user's TOTP, we need to enrol the user.

### Tuning configuration

## Contributions

Contributions to 1time are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Copyright (c) 2022 Atlassian and others.
Apache 2.0 licensed, see [LICENSE](LICENSE) file.

<br/> 


[![With â¤ï¸ from Atlassian](https://raw.githubusercontent.com/atlassian-internal/oss-assets/master/banner-cheers.png)](https://www.atlassian.com)
