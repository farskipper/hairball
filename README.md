# hairball - a Virtual DOM implementation in pure ClojureScript

This is not a React.js wrapper, it's a pure ClojureScript implementation of a similar approach to UI rendering. And by the way, it's implemented in fewer lines of code than most ClojureScript React.js wrappers.

The point of hairball is to prove how embracing pure functions and immutable data structures makes a reactive Virtual DOM library a cinch to write.

### Production ready?

No. At the moment this project is mostly experimental/educational. But there is no reason it couldn't become production ready.

In the mean time you should use one of these excellent React.js wrappers.
 * **[Reagent](http://reagent-project.github.io/)** - minimalist, very idiomatic to Clojure (hides the React API)
 * **[Om](https://github.com/omcljs/om)** - single global app-state, closer to React's API
 * **[Quiescent](https://github.com/levand/quiescent)** - lightweight, very flexible

## How to use hairball

Check out the [TodoMVC](https://github.com/smallhelm/hairball/blob/master/examples/todomvc/main.cljs).

## How hairball works

### Virtual DOM data structure
```clojure
(defrecord Vdom [type attrs children])
```
For example 
```html
<div>
	Hello <a href="#">World</a>
</div>
```
Is represented as
```clojure
(Vdom :div {}
  ["Hello World " (Vdom :a {:href "#"} "Hi")])
```
In practice you use syntactic sugar 
```clojure
(d/div {:class "hello"}
  "Hello " (d/a {:href "#"} "Hi"))
```

### (vdom->string vdom)

Simply take in a Virtual DOM and return an HTML string.


### (vdoms->JSops vdom1 vdom2); aka the diffing function

Given two vdom's (typically the old one and the new one) return a list of operations that should be applied to the DOM.

```clojure
(defrecord JSop [op path args])
```

For example
```clojure
(vdoms->JSops (d/div "before")
              (d/div "after"))

 > [(JSop :set-content [0] ["after"])]
```
another example
```clojure
(vdoms->JSops (d/div {:class "muted"} "before")
              (d/div {:class "selected"} (d/b "after")))

 > [(JSop :set-attribute [0] [:class "selected"])
    (JSop :set-content [0] [""])
    (JSop :insert-child [0] [(d/b "after") 0])]
```
Look at the [tests](https://github.com/smallhelm/hairball/blob/master/test/hairball/core_test.clj) for more examples.

### (apply-JSop-to-dom! jsop)

This function applies a JSop to the DOM.  For browser normalization this uses the Google Closure library.

BTW did you notice this is the first mention of an impure function? Did you further notice this all the other functions run both server-side and client-side?

### Event handlers

Similar to React, hairball mounts a single event listener to the root document. Then simulates event dispatch to your event handlers in the virtual DOM. Again this uses Google Closure library for browser normalization.

## License
The MIT License (MIT)

Copyright (c) 2015 Small Helm LLC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
