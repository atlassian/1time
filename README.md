> Please remove this quote block and replace the others with meaningful content. If you're looking at this on Bitbucket, be assured that it looks great on GitHub.

# [Project name]

[![Atlassian license](https://img.shields.io/badge/license-Apache%202.0-blue.svg?style=flat-square)](LICENSE) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](CONTRIBUTING.md)

> Provide an introduction/overview of your project, and articulate the problem it solves for the broader engineering community. Potential consumers should understand **what** the project is, and **why** it exists.

> Other recommended badges are a [version](https://shields.io/category/version) badge (e.g. `npm`) and a [build](https://shields.io/category/build) badge (e.g., `circleci` or `travis`). Order should be `license - version - build - PRs`. Please use the `flat-sqare` style.
>
> See e.g.
>
> [![npm version](https://img.shields.io/npm/v/react-beautiful-dnd.svg?style=flat-square)](https://www.npmjs.com/package/react-beautiful-dnd) [![npm version](https://img.shields.io/npm/v/@atlaskit/button.svg?style=flat-square)](https://www.npmjs.com/package/@atlaskit/button) [![Build Status](https://img.shields.io/travis/stricter/stricter/master?style=flat-square)](https://travis-ci.org/stricter/stricter)


## Usage

> Provide a simple and concise (code) example of your project. Consumers should understand **how** your project solves their problem.

```kotlin

fun myProvider(): SecretProvider = SecretProvider  { TOTPSecret.fromString("laksd") }

val a = DefaultTOTPService(
    secretProvider = { TOTPSecret.fromString("laksd") }
)

val b = DefaultTOTPService(
    secretProvider =  { TOTPSecret.fromString("laksd") }
)

```




## Installation

> Provide instructions on how to install and configure the project.

## Documentation

> If your project is small and simple enough, documentation can be added here. For larger projects, provide a link to where the documentation lives.

## Tests

> Describe and show how to run the tests with code examples.

## Contributions

Contributions to [Project name] are welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Copyright (c) 2022 Atlassian and others.
Apache 2.0 licensed, see [LICENSE](LICENSE) file.

<br/> 

> Pick one of the following:

[![With â¤ï¸ from Atlassian](https://raw.githubusercontent.com/atlassian-internal/oss-assets/master/banner-cheers.png)](https://www.atlassian.com)