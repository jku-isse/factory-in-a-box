# Shopfloorfrontend

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 8.2.1.

## Dealing with Certificate Authority Error

Due to the usage of a self-signed certificate, the browser won't trust the HTTPS connection to the backend. To make the REST calls work you once have to visit manually localhost:8080/orders (or any other route where the backend is currently running). The browser will warn you that the connection isn't save because the CA isn't known. Once you tell the browser to trust the site the REST calls in the frontend will also work.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.


## Publish on Apache server

If you use Apache to statically host the website, you have to put all the generated files from your build process in the `htdocs/` directory of your Apache distribution.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).