# OOK front-end architecture

OOK's front end is a single-page appp built with [re-frame](https://github.com/day8/re-frame). This is an overview of how it's structured and where to find things.

## Overview

- The UI is driven by the URL, which is handled by the reitit router in the `ook.reframe.router` namespace
- navigation is managed with the reframe events and effects registered in `ook.reframe.events` and `ook.reframe.<feature>.events` namespaces

1. navigate to a URL
2. router controllers (in `ook.reframe.router`) detect the URL and dispatch the relevant events
   - if the url is `/search` and there is a `filters` query param, dispatch an event to apply the filters
   - otherwise, navigate home, which will reset the dataset results to the full list


## `ook.reframe.router`

## Events

- events namespaced with `:ui.event/` are click handlers and called directly from views
- the rest of the events are triggered by these ones in one way or another
- there are 2 types of side-effecting events in the app
  1. http events, namespaced with `:http/`
  2. navigation, which is just the `:app/navigate` event and associated `:app/navigate!` effect, which uses reitit to change the app url
- Events, especially the side-effecting events, are written to do as little as possible on success or failure (usually just update some internal app state) so they can be reused in many places without causing surprise side effects. Use `:async-flow` to control what should happen after a handler resolves if more needs to be done than just setting the return value in the internal app state.

## Testing

Test setup utils are in `ook.test.util.setup`. Side-effecting handlers are stubbed here. Re-frame's built in `dispatch-later` effect is also stubbed here to simple dispatch the delayed event immediately. This works in our case because we only use `dispatch-later` to trigger UI updates after a small delay to prevent loading spinners from flashing in the UI for requests that happen quickly (<300ms).
