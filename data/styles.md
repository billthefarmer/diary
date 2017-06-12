## Styles
| **Markdown** | **HTML** | **Styles** |
| :-- | :-: | :-- |
| `# Header` | `<h1>Header</h1>` | `h1 { ... }`|
| `## Subheader` | `<h2>Subheader</h2>` | `h2 { ... }` |
| `text` | `<p>text</p>` | `p { ... }` |
| `* item` | `<ul><li>item</li></ul>` | `ul { ... }` |
| `1 one` | `<ol><li>one</li></ol>` | `ol { ... }` |
| `[google](google.com)` | `<a href="google.com">google</a>` | `a { ... }` |
| `![cat](cat.jpg)` | `<img src="cat.jpg" alt="cat">` | `img { ... }` |
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
