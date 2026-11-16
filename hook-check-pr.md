## Проверка хука `.claude/hooks/compile-check.sh`

В Java и TSX внесены заведомо неверные правки. Хук сработал на каждую правку (PostToolUse) и вернул агенту ошибку компилятора. Агент исправил обе ошибки по этим сообщениям; после исправления хук не выдал ничего, `git diff` пуст.

### Java: `rfr-e2e/.../util/DataUtils.java`

Правка: `randomString(PASSWORD_ALPHABET, 12)` → `randomString(PASSWORD_ALPHABET, "12")`.

```text
Compilation of rfr-e2e failed after editing rfr-e2e/src/test/java/io/student/rangiffler/util/DataUtils.java
/rfr/rfr-e2e/src/test/java/io/student/rangiffler/util/DataUtils.java:26: error: incompatible types: String cannot be converted to int
    return randomString(PASSWORD_ALPHABET, "12");
                                           ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
1 error

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':rfr-e2e:compileTestJava' (registered by plugin class 'org.gradle.api.plugins.JavaBasePlugin').
> Compilation failed; see the compiler output below.
  /rfr/rfr-e2e/src/test/java/io/student/rangiffler/util/DataUtils.java:26: error: incompatible types: String cannot be converted to int
      return randomString(PASSWORD_ALPHABET, "12");
                                             ^
  1 error

* Try:
> Check your code and dependencies to fix the compilation error(s)
> Run with --scan to get full insights from a Build Scan (powered by Develocity).

BUILD FAILED in 4s
```

Исправление: аргумент снова `12`; хук ничего не выдал.

### TSX: `rfr-gql-client/src/components/Loader/index.tsx`

Правка: `<CircularProgress color="primary" …>` → `<CircularProgress color="primay" …>`.

```text
Type check of rfr-gql-client failed after editing rfr-gql-client/src/components/Loader/index.tsx
src/components/Loader/index.tsx(3,47): error TS2820: Type '"primay"' is not assignable to type 'OverridableStringUnion<"primary" | "secondary" | "error" | "info" | "success" | "warning" | "inherit", CircularProgressPropsColorOverrides> | undefined'. Did you mean '"primary"'?
```

Исправление: цвет снова `"primary"`; хук ничего не выдал (`tsc` и ESLint прошли).
