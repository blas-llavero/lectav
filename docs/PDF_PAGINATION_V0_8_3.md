# LECTAV 0.8.3 — PDF typography and pagination

## Problems addressed

On an Android tablet, long educational documents showed crowded typography, overly wide table columns, orphaned headings and page breaks that separated a table from its heading or split important teaching content.

## Adjustments

PDF print styles were refined to use smaller readable fonts, more appropriate table widths, A4 page margins and semantic page boundaries. Headings are kept with following content where practical, and oversize blocks produce an explicit warning rather than being silently clipped.

The implementation still has finite layout and page limits. Very long tables and nested mathematical structures require additional manual inspection and may need the content to be split across smaller documents.

## QA checklist

Test headings followed by paragraphs, exercises containing equations, wide tables, long URLs, multiple-choice content and pages with differing text lengths. Verify no overlapping text, unreadable symbols, missing rows or unexpected blank pages. Ensure the document viewer renders all pages and export produces the same visible output.
