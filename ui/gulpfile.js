/*

=========================================================
* Pixel Pro Bootstrap 5 UI Kit
=========================================================

* Product Page: https://themesberg.com/product/ui-kit/pixel-pro-premium-bootstrap-5-ui-kit
* Copyright 2021 Themesberg (https://www.themesberg.com)

* Coded by https://themesberg.com

=========================================================

* The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. Contact us if you want to remove it.

*/

var autoprefixer = require('gulp-autoprefixer');
var browserSync = require('browser-sync').create();
var cleanCss = require('gulp-clean-css');
var del = require('del');
const htmlmin = require('gulp-htmlmin');
const cssbeautify = require('gulp-cssbeautify');
var gulp = require('gulp');
const npmDist = require('gulp-npm-dist');
var sass = require('gulp-sass')(require('sass'));
var wait = require('gulp-wait');
var sourcemaps = require('gulp-sourcemaps');
var fileinclude = require('gulp-file-include');

// Define paths

const paths = {
    dist: {
        base: './dist/',
        css: './src/main/resources/static/css/prod',
    },
    dev: {
        css: './src/main/resources/static/css/dev',
        templates: './src/main/resources/templates',
    },
    base: {
        base: './',
        node: './node_modules'
    },
    src: {
        base: './src/main/resources/static/',
        css: './src/main/resources/static/css',
        scss: './src/main/resources/static/scss',
        node_modules: './node_modules/',
    },
    temp: {
        base: './.temp/',
        css: './.temp/css',
    }
};

// Compile SCSS
gulp.task('scss', function () {
    return gulp.src([paths.src.scss + '/custom/**/*.scss', paths.src.scss + '/pixel/**/*.scss', paths.src.scss + '/pixel.scss'])
        .pipe(wait(500))
        .pipe(sourcemaps.init())
        .pipe(sass().on('error', sass.logError))
        .pipe(autoprefixer({
            overrideBrowserslist: ['> 1%']
        }))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(paths.temp.css))
        .pipe(browserSync.stream());
});

// Beautify CSS
gulp.task('beautify:css', function () {
    return gulp.src([
        paths.dev.css + '/pixel.css'
    ])
        .pipe(cssbeautify())
        .pipe(gulp.dest(paths.dev.css))
});

// Minify CSS
gulp.task('minify:css', function () {
    return gulp.src([
        paths.dist.css + '/pixel.css'
    ])
    .pipe(cleanCss())
    .pipe(gulp.dest(paths.dist.css))
});


// Clean
gulp.task('clean:dist', function () {
    return del([paths.dist.base]);
});

gulp.task('clean:dev', function () {
    return del([paths.dev.base]);
});

// Compile and copy scss/css
gulp.task('copy:dist:css', function () {
    return gulp.src([paths.src.scss + '/custom/**/*.scss', paths.src.scss + '/pixel/**/*.scss', paths.src.scss + '/pixel.scss'])
        .pipe(wait(500))
        .pipe(sourcemaps.init())
        .pipe(sass().on('error', sass.logError))
        .pipe(autoprefixer({
            overrideBrowserslist: ['> 1%']
        }))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(paths.dist.css))
});

gulp.task('copy:dev:css', function () {
    return gulp.src([paths.src.scss + '/custom/**/*.scss', paths.src.scss + '/pixel/**/*.scss', paths.src.scss + '/pixel.scss'])
        .pipe(wait(500))
        .pipe(sourcemaps.init())
        .pipe(sass().on('error', sass.logError))
        .pipe(autoprefixer({
            overrideBrowserslist: ['> 1%']
        }))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(paths.dev.css))
});


gulp.task('build:dev', gulp.series('copy:dev:css', 'beautify:css'));
gulp.task('build:dist', gulp.series('clean:dist', 'copy:dist:css', 'minify:css'));

// Watch for SCSS changes during development
gulp.task('watch', function () {
  
    // Initialize BrowserSync to serve files from the root directory
     browserSync.init({
       proxy: "localhost:8082", // Use your Spring Boot local server URL
       port: 3000
     });

     // Watch SCSS changes
        gulp.watch(paths.src.scss, gulp.series('copy:dev:css', 'beautify:css'));

     // Watch for CSS changes and reload the browser
      gulp.watch(paths.dev.css + '/**/*.css').on('change', browserSync.reload);
      gulp.watch(paths.dev.templates + '/**/*.html').on('change', browserSync.reload);
       
    
    
});
// Default
gulp.task('default', gulp.series('build:dist'));