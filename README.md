## WhatsThat
An android riddle game app.

#The idea
An android application created for and with Android-Studio. It's a cooperation between me and my 
brother and a playground for various mini games and ideas. All riddle types have the central idea
 to find the solution word to an arbitrary image provided and hidden by the riddle. Most riddles are
  not hard to solve and take from one to several minutes of play time. Some can be skipped and 
  solved later. The selection of the riddle type to play is up to the player, though one one new 
  riddle type can be chosen at each level that is gained by fulfilling achievements and solving 
  riddles.
#Working progress
As I am a student of mathematics and not directly into computer science or game development. This 
also is a playground for coding and image processing for me. My brother does not work much on the 
code but rather on the development of the ideas, collection of images, general game design and the 
testing.
As there are many free parameters to adjust for game balance and enjoyment and we are a 
little team the (beta) testing will probably take some time and we will see how and when it works 
out.
 
#How to get the app
As soon as major testing is done a release version will be available on the google play store 
free of charge. The project can be built using latest (Marshmallow) android SDK with a fully 
setup android studio.

#Contribution
If you find a bug, want to implement a new feature or riddle, feel free to do so. As this is a 
non profit project with no ads the only way to thank you will be adding you to the credits. 


* Create a new riddle
    1. In riddle/types/Types.java create a new static class for your riddle type and overwrite all 
    required (and optional) methods. See other riddle types in same file for examples.
    2. Create a singleton instance of your riddle type class in riddle/types/PracticalRiddleType
    .java and add that instance to the static global ALL_PLAYABLE_TYPES list in the same file.
    3. The method makeRiddle of your riddle type class needs to return a new instance of a 
    subclass of RiddleGame implementing your game logic. The game can react to motion events, to 
    device orientation events or simply periodically get refreshed with the period being the 
    device FPS rate.
    4. If the game is nice and playable you should make sure it can be restored from a saved 
    instance. Keep in mind that this should not produce high sized data and still restore 
    everything relevant for game flow.
    5. Optionally create a snapshot of the current game state in case the riddle is skipped.
    6. Add an achievement holder class for your riddle type in riddle/achievement/holders/. See 
    other type holders for examples. There the achievement logic is implemented. The achievement 
    holder is a singleton instantiated by the riddle type singleton.
    6. Test the game and the achievements so they produce the desired results.
    
* Create a shop article
    * A downloadable bundle can be created in the workshop by selecting a bunch of images and 
    filling out required information about the images. Do NOT use images that you hold no 
    copyright of or a licensed. The class testsubject/shopping/sortiment/SortimentHolder.java 
    holds all articles and their dependencies. Upload the bundle to some file hoster that gives out
    a static download link to the file (no captchas hiding the real link) and add a new 
    DownloadArticle to the list.
    * Some new article for some unlockable feature for a riddle can be implemented easily, but 
    the logic controlling the effect ingame needs to be added in the riddle game class and you 
    should take care in how it effects achievements or the game balance.
* Translations
    * One part would be the translation of app texts on buttons and text boxes which are located 
    in the resources folder. There are 4 string files offering a little thematic sorting of where
     the strings are used.
    * A bigger part would be the translation of the image solution words. This should be done 
    externally in some table calculation sheet like excel so it can be more easily managed and 
    merged by us. Write if you are interested in doing so and we will provide you with the 
    required resources. 
* Bugs, errors, misspellings
    If you find any of these just write a small note or email and we will fix them. In case of 
    bugs it would be very helpful to provide the generated log file in case of a crash or a 
    description of the situation in case something strange happened but the app did not crash. We
     are no native English speakers and therefore some formulations or spellings might be 
     misleading or incorrect.