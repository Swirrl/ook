# OOK front-end architecture

OOK's front end is a single-page appp built with [re-frame](https://github.com/day8/re-frame). This is an overview of how it's structured and where to find things.

## Overview

- The UI is driven by the URL, which is handled by the reitit router in the `ook.reframe.router` namespace
- navigation is managed with the reframe events and effects registered in `ook.reframe.events`

1. navigate to a URL
2. router controllers (in `ook.reframe.router`) detect the URL and dispatch the relevant events
   - if the url is `/search` and there is a `filters` query param, dispatch an event to apply the filters
   - otherwise, navigate home, which will reset the dataset results to the full list


## `ook.reframe.router`
