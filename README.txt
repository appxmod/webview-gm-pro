https://github.com/wbayer/webview-gm





# Updates
- More secure
- More Compatible
- More Fast
- More Functions



# Todo
- [feat] Mannually Execute Script
- [feat] Use Ace Code editor
- [feat] Connect to PC
- [doc] Update sample




# Notes

[dev] 编译需要下载 metaline 等支持库，解压到 maven local 中。
[dev] 需要重载一些方法才能使用。
[Compatiblity] 脚本里尽量不要用这种写法： `unsafeWindow`.GM_xxx，应该直接调用  GM_xx ：

```
if (typeof GM_xx !== 'undefined') GM_xx(……)；
```


# Warning

我已很少使用手机浏览器，本库不再维护。



# Detail
## More secure

>>>>>
    add security parameter `String secret, String name` to all inner calls to avoid illegal invocation.
    ede685ed004413d0e4a65a292f3ebb794859693e
    ```
    if (!this.secret.equals(secret)) { // security check
        return null
    }
    key.runtimeId // dynamic runtime identity
    ```

>>>>>
    new privilege system stored in bit flag.
    ScriptCriteria.java
```
    script.hasRightRunStart()  
    script.hasRightRequire()
    ……
```

>>>>>
    handle @connect correctly
    bfece3b7fe678cf20b1c050c2730d8f795597b4e



## More Compatible

>>>>>
    fix GM_xmlHttpRequest
    the original implementation is creative but buggy.



>>>>> 
    fix @match 
    fix CriterionMatcher：the matcher should be more strict. dot `.` should be used to differentiate domains.
    bb4c876b8ec14866807ea987540731129b05fa33



>>>>> 
    fix access unsafeWindow.GM_xxx
    951f21a8857c5b5c4a7cc6b32380ef04db98b7e3





## More Fast

>>>>>
    Optimize many code. to avoid OOM

>>>>>
    Avoid many `string + string` to avoid OOM
    f3244082d785eda75cbe53653766e599b8765d00

>>>>>
    Drop unsed resources and requires after edit code
    adae914c322ece3829b78a87d46df3a14d3b265f




## More Functions

>>>>>
    setClipboard getClipboard
    b2aeb7f1d984aa35fbd30eb8c686f1c708bf1b16


>>>>>
    menu support : list running script and their commands
    257bb1c11bae77ff6df1f6a4c1d7c586e8e4a782


>>>>>
    manage cookie


>>>>>
    GM_turnOnScreen
    GM_knock
    windowHeight
    GM_config


>>>>>
    implement `GM_openInTab` : you should inherit `WebViewGmApi` and write your own brower logic there.
    3c584ca156db32721f38fa08f8b946fa4e611ef5
    WebViewGmApi.java










