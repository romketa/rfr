---
name: page-object
description: How to write and change Page Objects and page components for the Playwright Java e2e tests in rfr-e2e. Use this skill whenever a page object, page component, locator, screen, form or "PO" is created or edited, whenever a new e2e/UI test needs a page that does not exist yet, and whenever existing page classes are reviewed or refactored, even if the user only says "add a page", "write a test for the people page" or "fix the locator".
---

# Page Objects in rfr-e2e

Page Objects live in `rfr-e2e/src/test/java/io/student/rangiffler/page`, reusable pieces of a page in
`page/component`. Tests chain page methods and never touch `Page` or locators directly, so every rule below
exists to keep that chain readable and to make a failure point at one method.

Copy the structure from the templates instead of inventing one:
- `templates/ExamplePage.java` is a full page: URL constant, locators, `open`, action methods, check methods.
- `templates/ExampleComponent.java` is a reusable fragment (header, table row, dialog) that pages compose.

## Rules and why they exist

**`final` classes, composition instead of inheritance.** A page is a leaf: nothing extends it, so declare it
`final`. Shared behaviour (the app header, a search box, a confirmation dialog) is a component class the page holds
as a field and exposes through a method such as `header()`. An inheritance tree (`BasePage` with abstract hooks and the
like) couples every page to one abstract contract and hides which page a locator belongs to; a component makes the
reuse explicit and testable on its own.

**One user action is one method.** An action is what the user does in one go on the page: fill a form, click a
button, follow a link, pick a value. So a page has `fillLoginForm(username, password)`, `clickSignIn()`,
`clickRegister()`, and a test chains them: `.fillRegistrationForm(u, p).clickSignUp().checkSuccessMessage()`. Two
granularities are wrong. Finer (`setUsername`, `setPassword`) moves the form logic into the test and floods the
report. Coarser (`register(u, p)` that fills and submits) hides the click inside the fill, so a failure report says
"register" instead of which action broke, and the test cannot assert anything between the two (a validation message
before submit, a disabled button). A whole form is still one action because the user fills it as a unit and the fields
only make sense together. Action methods return the page the user lands on (`this` or a new page object) so tests read
as a chain; name them by the verb and the control: `fill<Form>Form`, `click<Button>`, `select<Field>`.

**Check methods contain only checks.** A method whose name starts with `check` asserts and returns `this`; it never
clicks, fills, navigates or waits for something else to happen. Mixing an action into a check makes the Allure step
lie about what failed. Use Playwright assertions (`PlaywrightAssertions.assertThat(locator).isVisible()`,
`hasText`, `hasURL`...) because they retry until the timeout from `Config.actionTimeoutMs()` and so absorb rendering
delays; use JUnit assertions only for state Playwright cannot assert on (for example a value read from
`localStorage`) and explain the exception in a comment.

**No "page loaded" checks.** Do not write `checkThatPageLoaded`, `waitForLoad` or similar methods. Playwright actions
wait for their target to be actionable and web-first assertions retry until the timeout, so the first real action or
check on a page already waits for it; a separate readiness check adds a step that asserts nothing the test is about and
doubles the wait on failure. Every `check*` method asserts a business outcome (a message, a value, a row, a URL).

**Every public action and check method has `@Step` with its parameters in the text.** Allure is the only place where a
failed run is read, so each step names what it did and with what: `@Step("Register user {username}")`,
`@Step("Open photo of {country}")`. Leave secrets (passwords, tokens) out of the step text. A plain accessor that
returns a composed component (`header()`) does nothing in the browser and gets no `@Step`: an empty step only hides
the real ones.

**The page URL is `public static final String URL`.** Built from `Config.frontUrl()` or `Config.authUrl()` so the
stand can be switched with a property or an environment variable. `open()` navigates to `URL` and returns `this`.

**The constructor takes `Page` and only assigns fields.** Its body is `this.page = page` plus the initialisation of the
`private final` fields: locators and composed components. It never navigates, waits, asserts or changes browser
settings, because a page object is also created for a page the test has not opened yet (`login()` returns
`new MyTravelsPage(page)` before the redirect finishes) and because a constructor with side effects cannot be chained
or reported as a step. Opening belongs to `open()`.

**Locators are `private final` fields initialised in the constructor**, named after what the user sees
(`loginButton`, `successMessage`). Prefer role and label based locators (`getByRole`, `getByLabel`); fall back
to ids or css only when the markup gives no accessible name (the Thymeleaf auth pages, for instance). Inside a
component, derive every locator from the component root (`page.getByRole(AriaRole.BANNER)` for the header) because the
same button or link usually appears elsewhere on the page and an unscoped locator fails strict mode.

## Checklist before finishing

- Class is `final`, no `extends`, reusable parts are components held in fields.
- Constructor takes `Page`, assigns it and initialises `private final` fields, nothing else.
- `public static final String URL` present, `open()` uses it.
- Each action method is one user action (fill a form, click a control) and returns a page object.
- Each `check*` method only asserts, with Playwright assertions, and returns `this`.
- No `checkThatPageLoaded`-style methods; checks assert business outcomes only.
- Every public action and check method has `@Step` with parameters, no secrets in the text; component accessors have none.
- Test code calls only page object methods; `Page` appears in tests only as the injected parameter.

## Example chain in a test

```java
new LandingPage(page)
    .open()
    .clickRegister()
    .fillRegistrationForm(username, password)
    .clickSignUp()
    .checkSuccessMessage();
```
