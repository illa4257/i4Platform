# i4Platform

**i4Platform** is a development platform that includes a variety of utilities, the UI framework, 
and the programming language.

[Wiki](https://github.com/illa4257/i4Platform/wiki)

## Overview

- **i4Utils**: Universal classes like SyncVar, MutableCharArray and etc.
- **i4Framework**: UI Framework (requires `i4Utils`).
  - **i4Framework.desktop**: Some desktop universal utilities (Requires `jna-plaform`).
    - **i4Framework.swing**: Swing support for `i4Framework`.
  - **i4Framework.android**: Android support for `i4Framework`.
- **i4LCommon**: Classes for the programming language. (WIP)
- **i4LParser**: Parser for the programming language (requires `i4LCommon`).

## Usage
### i4Utils
```Java
final SyncVar<String> v = new SyncVar("Hello, world!");
System.out.println(v.get()); // Hello, world!
v.set("test");
System.out.println(v.get()); // test
```

### i4Framework + i4Framework.swing
```Java
import illa4257.i4Framework.base.*;
import illa4257.i4Framework.base.components.Window;
import illa4257.i4Framework.base.events.components.StyleUpdateEvent;
import illa4257.i4Framework.base.points.PPointSubtract;
import illa4257.i4Framework.base.points.numbers.NumberPointMultiplier;
import illa4257.i4Framework.base.utils.CSSParser;
import illa4257.i4Framework.swing.SwingFramework;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Test {
    public static void main(final String[] args) {
        final Framework framework = SwingFramework.INSTANCE;
        /*
        final FrameworkWindow frameworkWindow = framework.newWindow(null);
        final Window window = frameworkWindow.getWindow();
        
        OR
         */
        
        final Window window = new Window();
        final FrameworkWindow frameworkWindow = framework.newWindow(window);

        // Add styles
        try (final InputStream is = framework.openResource("assets:///illa4257/i4Framework/light.css")) {
            if (is != null)
                // Parse and apply the CSS stylesheet to the window
                CSSParser.parse(window.stylesheet, new BufferedReader(new InputStreamReader(is)));
                // Or you can load directly into Framework's stylesheet
                // CSSParser.parse(framework.stylesheet, new BufferedReader(new InputStreamReader(is)));
        }

        window.setSize(720, 480);

        final Button btn = new Button("Click me!");
        btn.setX(8, Unit.DP);
        btn.setY(8, Unit.DP);
        
        // Set the width of the button dynamically to (window width - 8 dp)
        btn.setEndX(new PPointSubtract(window.width, new NumberPointMultiplier(window.densityMultiplier, 8)));
        
        btn.setHeight(32, Unit.DP);
        btn.addEventListener(ActionEvent.class, e -> System.out.println("Hello, world!"));
        window.add(btn);

        window.center();
        window.setVisible(true);
    }
}
```

## Contributing
I'm currently developing this project solo, but I welcome feedback, suggestions, and pull requests.
If you find any bugs or have ideas for improvements, feel free to create a GitHub issue or submit a pull request.

## Requirements
 - Java 8 or higher
