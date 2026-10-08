# Aquarium configuration

Settings are saved immediately in private Android preferences. The launch
activity and idle DreamService both read the same configuration. Settings use
validated bounds, and unreadable preference types fall back to defaults.

## Population and species

| Population | Fish count | Swimming behavior |
|---|---|---|
| Custom | 0-60 | Independent paths |
| A few | 6 | Independent paths |
| A handful | 16 | Independent paths |
| A lot | 32 | Independent paths |
| A ton | 60 | Independent paths |
| Schools | 48 | Three coordinated groups of 16 |

Changing the exact count selects Custom. Schools share direction and formation
within each group, wrap together beyond the tank edges, and use smaller fish to
keep the formation spaced. Mixed schools use a different species per group.
Other Mixed populations distribute all six species across individual fish.

Available species are Clownfish, Yellow tang, Blue tang, Angelfish, Neon tetra,
and Betta. Size and speed have three choices each. Reset restores 16 mixed fish,
Reef, Gentle swimming, Medium size, Daylight, bubbles and shimmer enabled, hidden
clock, Realistic appearance, and all optional sea life disabled.

## Appearance

Choose Look immediately below Mode. Every animated appearance supports all five
backgrounds, every population preset and species, and all seven sea-life switches.
Changing Look retains the other aquarium settings.

| Look | Appearance |
|---|---|
| Realistic | Detailed natural fish artwork and realistic underwater scenery |
| Classic Windows aquarium | Natural fish with a cool aquarium palette and subtle glass framing |
| Animated 3D | Polished cinematic 3D artwork with soft underwater depth |
| Cartoon | The original simple 3D fish, plants, rocks, and coral |
| Finding Nemo inspired | Expressive cinematic fish, colorful reef scenery, and a warmer vivid palette |
| Little Mermaid inspired | Classic drawn fish and matching painted underwater backgrounds |

The cinematic and drawn appearances use original animals and scenery. Look names
describe their visual direction. The same fish can swim left or right with
animated tails and fins. Octopuses and jellyfish have flowing movement.
See [Artwork](ARTWORK.md) for rendering and source-resolution details.

## Backgrounds

| Background | Environment |
|---|---|
| Reef | Branching coral, rocks, sand, blue water |
| Fish tank | Plants, substrate, tank edges, filter silhouette |
| Ocean | Open blue water without tank furniture |
| Kelp forest | Tall moving plants, rocks, green water |
| Deep sea | Dark open water |

Each background has realistic, cinematic, and drawn artwork. Cartoon uses original
geometry. Animals remain available in every background so you can create your
own combinations.

## Optional sea life

Each switch controls a separate group, in addition to the ordinary fish count:

| Switch | Animals |
|---|---|
| Sharks | Two swimming sharks |
| Whales | One large whale |
| Octopuses | One octopus with eight animated arms |
| Turtles | One swimming turtle |
| Rays | One ray |
| Dolphins | Two dolphins |
| Jellyfish | Five drifting jellyfish |

The animals use distinct shapes, sizes, colors, and appendages in the selected
appearance, while footage mode shows a real recorded aquarium.

## Light, water, and clock

Daylight, Warm, and Moonlight change the aquarium's lighting. Sunlight shimmer
adds moving beams from above and rippling highlights on surfaces facing the
light. It is independent of the lighting choice and bubbles.

The clock uses the device's local time and time zone. Choose Hidden, 12-hour,
or 24-hour. It works with both animated and footage modes.

## Footage mode

**Original 4K footage** preserves the exact aquarium video from the first
installed version and plays it muted on a loop. Fish count, species,
environment, lighting, and sea-life controls are disabled while footage plays.
Their saved values are retained and restored when returning to animated mode.

## Day and night

**Day / Night** works with every animated look and the original video. Day keeps
normal brightness. Night applies a dark overlay that reduces the aquarium and
clock to 40% brightness. Your choice is saved for preview and idle screensaving.
The lighting color and sunlight shimmer remain independent settings. This is a
manual switch and does not change the TV's system brightness or idle timeouts.
The clock remains available.

## Random selection

Use Left / Right or Select to reach Random after the fixed options on any
setting. Mode offers Random version, which selects one enabled Shuffle version
with equal probability. Seven switches at the bottom of settings include or
exclude each animated look and the original footage. At least one stays enabled.
Look is selected by that shuffle and its separate control is disabled while
Random version is active. To shuffle only animated looks, keep Mode on Animated
aquarium and choose Look: Random instead.

Random background, species, speed, size, lighting, bubbles, shimmer, each sea-life
switch, clock, and Day / Night are independent. Keep Day / Night on Night for a
consistently dim aquarium even when the other settings shuffle.

Population and exact Fish count describe the same population: adjusting Fish
selects Custom and clears random Population; selecting a preset or Random
Population clears random Fish. Random Population chooses among A few, A handful,
A lot, A ton, and Schools with their defined counts. Random Fish chooses 0-60.

Values are resolved when the app opens, when settings change, when Show aquarium
is pressed, and when a dream starts. They remain stable during that showing;
randomization does not run every frame. Resolved values never overwrite saved
Random choices. Fixed settings and excluded versions remain respected on later
showings. A random result can repeat by chance.
