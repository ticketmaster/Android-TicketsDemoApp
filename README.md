# Android Tickets SDK Application Integration Demo

This is an example integration of the Ticketmaster Ignite SDK, Tickets framework.

* Overview: https://business.ticketmaster.com/ignite/
* Documentation: https://ignite.ticketmaster.com/docs/tickets-sdk-overview
* Android Source (Tickets SDK): https://github.com/ticketmaster/Android-TicketsDemoApp
* iOS Source (Tickets SDK): https://github.com/ticketmaster/iOS-TicketsDemoApp

## Demo App Screenshots

<img src="screenshots/sample_integration_app_1.jpg" alt="Getting Started" /> <img src="screenshots/sample_integration_app_2.jpg" alt="Login" /> <img src="screenshots/sample_integration_app_4.jpg" alt="Tickets Listing Page" /> 


## Getting Started
1. Open Android-TicketsDemoApp in Android Studio
    1. This will also download all the required libraries

2. Update local.properties with your own API key

   1. available from https://developer.ticketmaster.com/explore/

3. Update your Team Name and colors:

   `config.consumer_key="consumer_key"`
   `config.team_name="team_name"`
   `config.branding_color="#color"`

4. Build and Run

# Example Code

## Configuration
Update your API key, team name and branding colors in local.properties
Authentication SDK is configured using the settings in local.properties.
Tickets SDK inherits it's configuration from Authentication SDK

## Presentation
There is one way to present the Tickets SDK:

Generate the EventsFragment from TicketsSDKClient and add it to your view.

Basic example in TicketsSDKHostActivity.kt

## Authentication

While not required, your application may want to control login-related processes directly.

* **Login**
* **Member Info**
* **Logout**

Tickets SDK handles Login/Logout on it's own, so there is no need for you to manually call any of these methods.


## Information

While not required, your application may want to be informed of operations and use behavior with Authentication and Tickets SDKs.

This information is provided via delegate protocols, basic examples are provided.


## Custom Modules


While not required, your application may want to use Prebuilt Modules or even create your Custom Modules to display underneath the Tickets on the Tickets Listing page.

