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
clock, and all optional sea life disabled.

## Backgrounds

| Background | Environment |
|---|---|
| Reef | Branching coral, rocks, sand, blue water |
| Fish tank | Plants, substrate, tank edges, filter silhouette |
| Ocean | Open blue water without tank furniture |
| Kelp forest | Tall moving plants, rocks, green water |
| Deep sea | Dark open water |

The environments are stylized original geometry. Animals remain available in
every background so you can create your own combinations.

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

The models use distinct shapes, sizes, colors, and appendages. The animated
mode is stylized 3D, while footage mode shows a real recorded aquarium.

## Light, water, and clock

Daylight, Warm, and Moonlight change the aquarium's lighting. Sunlight shimmer
adds moving beams from above and rippling highlights on surfaces facing the
light. It is independent of the lighting choice and bubbles.

The clock uses the device's local time and time zone. Choose Hidden, 12-hour,
or 24-hour. It works with both animated and footage modes.

## Footage mode

Real 4K footage plays the bundled muted video on a loop. Fish count, species,
environment, lighting, and sea-life controls are disabled while footage plays.
Their saved values are retained and restored when returning to animated mode.
The clock remains available.
