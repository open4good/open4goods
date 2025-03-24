// Imports
import autoprefixer from 'gulp-autoprefixer';
import browserSyncPkg from 'browser-sync'; // Live browser reloading
import cleanCss from 'gulp-clean-css'; // Minify CSS
import { deleteAsync } from 'del'; // Delete files/folders
import cssbeautify from 'gulp-cssbeautify'; // Beautify CSS
import gulp from 'gulp'; // Main Gulp library
import sourcemaps from 'gulp-sourcemaps'; // Generate source maps
import uglify from 'gulp-uglify'; // Minify JavaScript
import rename from 'gulp-rename'; // Rename files
import gulpSass from 'gulp-sass'; // Compile SCSS to CSS
import * as dartSass from 'sass'; // Dart Sass as the compiler for gulp-sass

// Use Dart Sass
const sass = gulpSass(dartSass);

// Initialize BrowserSync
const browserSync = browserSyncPkg.create();

// Define paths for better maintainability
const paths = {
    vendor: './src/main/resources/static/vendor/',
    dist: {
        base: './dist/',
        css: './src/main/resources/static/css/prod',
    },
    dev: {
        css: './src/main/resources/static/css/dev',
        templates: './src/main/resources/templates',
    },
    src: {
        scss: './src/main/resources/static/scss',
        base: './src/main/resources/static/',
        js: './src/main/resources/static/js',
    },
};

// Compile SCSS to CSS with sourcemaps and autoprefixing
gulp.task('scss', function () {
    return gulp
        .src([`${paths.src.scss}/**/*.scss`]) // Source all SCSS files
        .pipe(sourcemaps.init())
        .pipe(sass().on('error', sass.logError)) // Compile SCSS
        .pipe(autoprefixer({ overrideBrowserslist: ['> 1%'] })) // Add browser prefixes
        .pipe(sourcemaps.write('.')) // Write sourcemaps
        .pipe(gulp.dest(paths.dev.css)) // Save in dev folder
        .pipe(browserSync.stream()); // Stream updates to BrowserSync
});

// Beautify CSS files
gulp.task('beautify:css', function () {
    return gulp
        .src([`${paths.dev.css}/**/*.css`]) // Select CSS files
        .pipe(cssbeautify()) // Beautify CSS
        .pipe(gulp.dest(paths.dev.css)); // Save beautified CSS
});

// Minify CSS files for production
gulp.task('minify:css', function () {
    return gulp
        .src([`${paths.dist.css}/**/*.css`]) // Select production CSS files
        .pipe(cleanCss()) // Minify CSS
        .pipe(gulp.dest(paths.dist.css)); // Save minified CSS
});

// Clean production folder
gulp.task('clean:dist', function () {
    return deleteAsync([paths.dist.base]); // Delete production folder
});

// Copy and process SCSS files for production
gulp.task('copy:dist:css', function () {
    return gulp
        .src([`${paths.src.scss}/**/*.scss`]) // Source SCSS files
        .pipe(sourcemaps.init())
        .pipe(sass().on('error', sass.logError))
        .pipe(autoprefixer({ overrideBrowserslist: ['> 1%'] }))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(paths.dist.css)); // Save to production CSS folder
});

// Minify JavaScript (e.g., pixel.js)
gulp.task('minify:pixel', function () {
    return gulp
        .src([`${paths.src.base}/assets/js/pixel.js`]) // Select JS file
        .pipe(sourcemaps.init())
        .pipe(uglify()) // Minify JavaScript
        .pipe(rename({ suffix: '.min' })) // Add ".min" suffix
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(paths.src.js)); // Save minified JS
});

// Copy vendor JavaScript files (npmDist removed for simplicity; adjust if necessary)
gulp.task('copy:minjs', function () {
    return gulp
        .src(['./node_modules/**/*min.js'], { base: './node_modules' }) // Copy minified JS files
        .pipe(gulp.dest(paths.vendor));
});

// Task to copy *.min.css files to the production directory
gulp.task('copy:mincss', function () {
    return gulp
        .src(['./node_modules/**/*min.css'], { base: './node_modules' }) // Copy minified JS files
        .pipe(gulp.dest(paths.vendor)); // Copy them to the production CSS directory
});

// Task to copy *.min.css files to the production directory
gulp.task('copy:minttf', function () {
    return gulp
        .src(['./node_modules/**/*.ttf'], { base: './node_modules' }) // Copy minified JS files
        .pipe(gulp.dest(paths.vendor)); // Copy them to the production CSS directory
});

// Task to copy *.min.css files to the production directory
gulp.task('copy:minwoff', function () {
    return gulp
        .src(['./node_modules/**/*.woff2'], { base: './node_modules' }) // Copy minified JS files
        .pipe(gulp.dest(paths.vendor)); // Copy them to the production CSS directory
});



// Watch files for changes
gulp.task('watch', function () {
    browserSync.init({
        proxy: "localhost:8082", // Proxy the development server
        port: 3000,
    });

    // Watch SCSS files and compile to CSS
    gulp.watch(`${paths.src.scss}/**/*.scss`, gulp.series('scss'));

    // Reload browser on CSS and HTML changes
    gulp.watch(`${paths.dev.css}/**/*.css`).on('change', browserSync.reload);
    gulp.watch(`${paths.dev.templates}/**/*.html`).on('change', browserSync.reload);
});



// Build distribution task
gulp.task('build:dist', gulp.series(
    'clean:dist',    // Clean previous builds
    'copy:dist:css', // Copy and process SCSS files
    'minify:css',    // Minify CSS files
    'copy:mincss',   // Copy minified CSS files
    'copy:minjs',    // Copy minified JavaScript files
    'scss',
//    'copy:minttf',    // Copy minified JavaScript files
//    'copy:minwoff',    // Copy minified JavaScript files
    
    'minify:pixel'   // Minify pixel.js
));
// Default build task
gulp.task('build', gulp.series('build:dist'));

// Default task
gulp.task('default', gulp.series('build'));
