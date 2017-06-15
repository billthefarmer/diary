## Styles
<table>
<tr><td><b>Markdown</b></td><td><b>HTML</b></td><td><b>Styles</b></td></tr>
<tr><td><code># Header</code></td><td><h1>Header</h1></td><td><code>h1 { ... }</code></td></tr>
<tr><td><code>## Subheader</code></td><td><h2>Subheader</h2></td><td><code>h2 { ... }</code></td></tr>
<tr><td><code>text</code></td><td><p>text</p></td><td><code>p { ... }</code></td></tr>
<tr><td><code>* item</code></td><td><ul><li>item</li></ul></td><td><code>ul { ... }</code></td></tr>
<tr><td><code>1 one</code></td><td><ol><li>one</li></ol></td><td><code>ol { ... }</code></td></tr>
<tr><td><code>[Google](google.com)</code></td><td><a href="http://google.com">Google</a></td><td><code>a { ... }</code></td></tr>
<tr><td><code>![cat](cat.jpg)</code></td><td><img src="cat.jpg" alt="cat"></td><td><code>img { ... }</code></td></tr>
</table>

### Example

    @import url("file:///android_asset/styles.css");
    img {
        max-width: 80%;
    }
    h2 {
        background: lightblue;
    }
    ol {
        background: yellow;
    }
