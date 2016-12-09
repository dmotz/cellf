(ns cellf.strings)

(def strings
 {
  :cam-denied "Sorry, Cellf doesn't work without camera access."
  :cam-failed (str
                "Sorry, it looks like your device or browser doesn't support camera access. "
                "Try Cellf using Chrome or Firefox on a device that supports WebRTC.")
  :intro1 (str
            "Cellf is an interactive experiment that reflects you and your "
            "surroundings as you play. When you click OK, Cellf will ask for camera access.")
  :intro2 (str
            "There's no server or multiplayer component to this: "
            "your image stays on your device.")
  :gif-result "Click this gif to save it. Share your Cellf with the world."
  :win-info "For more of a challenge, drag the slider to create a bigger grid."
  :how-to1 (str
             "Simply click a cell next to the empty cell to move it. "
             "When you shuffle them into the correct order, you win.")
  :how-to2 (str
             "You can also export a replay of your moves to an animated gif by "
             "clicking the 'make gif' button.")
  :about1 "Cellf was created by Dan Motzenbecker and is "
  :about2 " on Github. For more experiments like this, visit "})
