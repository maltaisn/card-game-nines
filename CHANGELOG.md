## v1.1.1

- Update target Android API, libGDX & other libraries.
- Added view source on GitHub button in about page.

## v1.1.0
- AI fixes and improvements
    - Fixed cheating bug. The known hand IDs was never reset, so the player
    kept knowing more and more hands as more rounds were played.
    - The "forgetting" algorithm for cards used random cards on every simulation,
    which probably resulted in no forgetting at all. Known and unknown cards are
    now fixed for a whole round.
    - Known suits was never updated so it was basically useless until now.
    - Added weighting heuristic for hand trading. Effect on play level is barely noticeable.
    - Smaller cards will be played first if many moves are equally visited. This aims
    to improve perception of stronger play rather that actually improve play level.
    - Rebalanced AI difficulties, all of them should be somewhat better.
    
- Fixed starting score preference change not being reflected when there was no saved game.
- Fixed blocked play after clicking on unplayable card.
- Fixed crash when clicking player cards during hide transition.

### v1.0.2
- Fixed crash with msdf-gdx on devices with older Open GL ES versions.

### v1.0.1
- Fixed crash when player card is clicked during trade phase.
- Updated coroutines to 1.3.2

# v1.0.0
- Initial release
