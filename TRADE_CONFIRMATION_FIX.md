# Trade confirmation label fix

The cached interface 334 has a built-in "Trading with:" heading in component 53 and a separate name field in component 54. TradeSession previously inserted another heading and a line break into component 54, overlapping the existing heading. openSecondTradeScreen now sends only the formatted partner name to component 54, retaining the cache-defined cyan color and shadow.

Validation: single-source Java 8-target compilation passed against existing runtime dependencies. Both generated TradeSession classes were staged in bin and their SHA-256 hashes matched the compilation outputs. Source and previous runtime classes are backed up under build/trade-confirmation-before. No trade transaction logic or client/cache files changed.

Restart the server to load staged classes. Live visual acceptance remains: open a trade confirmation on both accounts and verify one heading and one correctly formatted partner name.
