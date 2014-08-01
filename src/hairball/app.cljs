(ns hairball.app)

(def app-state (atom {}))

(defn app-swap! [path value]
  (swap! app-state assoc-in path value))

(defn app-get [path]
  (get-in @app-state path))
