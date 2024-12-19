# Add tabs

HTML elements are rendered with dokka. Therefore, we can use this div to create tabs, remember to change `tabs-0`:

```
<div class="tabs">
<input class="tabs_control hidden" type="radio" id="tabs-0-receiver-0" name="tabs-0" checked>
<label class="tabs_label" for="tabs-0-receiver-0">Kotlin</label>
<div class="tabs_content">
<!-- CONTENT KOTLIN -->
<!-- FIN KOTLIN -->
</div>
<input class="tabs_control hidden" type="radio" id="tabs-0-receiver-1" name="tabs-0">
<label class="tabs_label" for="tabs-0-receiver-1">Java</label>
<div class="tabs_content">
<!-- CONTENT JAVA -->
<!-- FIN JAVA -->
</div>
</div>
```

Thanks [geekdoc](https://geekdocs.de/), for the CSS.
