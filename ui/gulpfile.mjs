import autoprefixer from 'gulp-autoprefixer';
import browserSyncPkg from 'browser-sync';
import cleanCss from 'gulp-clean-css';
import { deleteAsync } from 'del';
import htmlmin from 'gulp-htmlmin';
import cssbeautify from 'gulp-cssbeautify';
import gulp from 'gulp';
import npmDist from 'gulp-npm-dist';
import wait from 'gulp-wait';
import sourcemaps from 'gulp-sourcemaps';
import fileinclude from 'gulp-file-include';

import gulpSass from 'gulp-sass';
import * as dartSass from 'sass';
const sass = gulpSass(dartSass);

const browserSync = browserSyncPkg.create();

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

// Tasks
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

gulp.task('beautify:css', function () {
    return gulp.src([
        paths.dev.css + '/pixel.css'
    ])
        .pipe(cssbeautify())
        .pipe(gulp.dest(paths.dev.css))
});

gulp.task('minify:css', function () {
    return gulp.src([
        paths.dist.css + '/pixel.css'
    ])
        .pipe(cleanCss())
        .pipe(gulp.dest(paths.dist.css))
});

gulp.task('clean:dist', function () {
    return deleteAsync([paths.dist.base]);
});

gulp.task('clean:dev', function () {
    return deleteAsync([paths.dev.base]);
});

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

gulp.task('watch', function () {
    browserSync.init({
        proxy: "localhost:8082",
        port: 3000
    });

    gulp.watch(paths.src.scss, gulp.series('copy:dev:css', 'beautify:css'));
    gulp.watch(paths.dev.css + '/**/*.css').on('change', browserSync.reload);
    gulp.watch(paths.dev.templates + '/**/*.html').on('change', browserSync.reload);
});

gulp.task('default', gulp.series('build:dist'));
