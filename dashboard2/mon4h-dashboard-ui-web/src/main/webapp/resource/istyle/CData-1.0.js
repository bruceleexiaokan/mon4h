/** vim: et:ts=4:sw=4:sts=4
 * @license RequireJS 2.1.5 Copyright (c) 2010-2012, The Dojo Foundation All Rights Reserved.
 * Available via the MIT or new BSD license.
 * see: http://github.com/jrburke/requirejs for details
 */
//Not using strict: uneven strict support in browsers, #392, and causes
//problems with requirejs.exec()/transpiler plugins that may not be strict.
/*jslint regexp: true, nomen: true, sloppy: true */
/*global window, navigator, document, importScripts, setTimeout, opera */


var IP = "127.0.0.1";
var sendmailPort = "9996";
var toCSVPort = "9995";
var webservicePort = "8080";
var feedmailPort = "undefined";
var requirejs, require, define,CData,jqueryReady=false;
(function (global) {
    var req, s, head, baseElement, dataMain, src,
        interactiveScript, currentlyAddingScript, mainScript, subPath,
        version = '2.1.5',
        commentRegExp = /(\/\*([\s\S]*?)\*\/|([^:]|^)\/\/(.*)$)/mg,
        cjsRequireRegExp = /[^.]\s*require\s*\(\s*["']([^'"\s]+)["']\s*\)/g,
        jsSuffixRegExp = /\.js$/,
        currDirRegExp = /^\.\//,
        op = Object.prototype,
        ostring = op.toString,
        hasOwn = op.hasOwnProperty,
        ap = Array.prototype,
        apsp = ap.splice,
        isBrowser = !!(typeof window !== 'undefined' && navigator && window.document),
        isWebWorker = !isBrowser && typeof importScripts !== 'undefined',
        //PS3 indicates loaded and complete, but need to wait for complete
        //specifically. Sequence is 'loading', 'loaded', execution,
        // then 'complete'. The UA check is unfortunate, but not sure how
        //to feature test w/o causing perf issues.
        readyRegExp = isBrowser && navigator.platform === 'PLAYSTATION 3' ?
                      /^complete$/ : /^(complete|loaded)$/,
        defContextName = '_',
        //Oh the tragedy, detecting opera. See the usage of isOpera for reason.
        isOpera = typeof opera !== 'undefined' && opera.toString() === '[object Opera]',
        contexts = {},
        cfg = {},
        globalDefQueue = [],
        useInteractive = false;

    function isFunction(it) {
        return ostring.call(it) === '[object Function]';
    }

    function isArray(it) {
        return ostring.call(it) === '[object Array]';
    }

    /**
     * Helper function for iterating over an array. If the func returns
     * a true value, it will break out of the loop.
     */
    function each(ary, func) {
        if (ary) {
            var i;
            for (i = 0; i < ary.length; i += 1) {
                if (ary[i] && func(ary[i], i, ary)) {
                    break;
                }
            }
        }
    }

    /**
     * Helper function for iterating over an array backwards. If the func
     * returns a true value, it will break out of the loop.
     */
    function eachReverse(ary, func) {
        if (ary) {
            var i;
            for (i = ary.length - 1; i > -1; i -= 1) {
                if (ary[i] && func(ary[i], i, ary)) {
                    break;
                }
            }
        }
    }

    function hasProp(obj, prop) {
        return hasOwn.call(obj, prop);
    }

    function getOwn(obj, prop) {
        return hasProp(obj, prop) && obj[prop];
    }

    /**
     * Cycles over properties in an object and calls a function for each
     * property value. If the function returns a truthy value, then the
     * iteration is stopped.
     */
    function eachProp(obj, func) {
        var prop;
        for (prop in obj) {
            if (hasProp(obj, prop)) {
                if (func(obj[prop], prop)) {
                    break;
                }
            }
        }
    }

    /**
     * Simple function to mix in properties from source into target,
     * but only if target does not already have a property of the same name.
     */
    function mixin(target, source, force, deepStringMixin) {
        if (source) {
            eachProp(source, function (value, prop) {
                if (force || !hasProp(target, prop)) {
                    if (deepStringMixin && typeof value !== 'string') {
                        if (!target[prop]) {
                            target[prop] = {};
                        }
                        mixin(target[prop], value, force, deepStringMixin);
                    } else {
                        target[prop] = value;
                    }
                }
            });
        }
        return target;
    }

    //Similar to Function.prototype.bind, but the 'this' object is specified
    //first, since it is easier to read/figure out what 'this' will be.
    function bind(obj, fn) {
        return function () {
            return fn.apply(obj, arguments);
        };
    }

    function scripts() {
        return document.getElementsByTagName('script');
    }

    //Allow getting a global that expressed in
    //dot notation, like 'a.b.c'.
    function getGlobal(value) {
        if (!value) {
            return value;
        }
        var g = global;
        each(value.split('.'), function (part) {
            g = g[part];
        });
        return g;
    }

    /**
     * Constructs an error with a pointer to an URL with more information.
     * @param {String} id the error ID that maps to an ID on a web page.
     * @param {String} message human readable error.
     * @param {Error} [err] the original error, if there is one.
     *
     * @returns {Error}
     */
    function makeError(id, msg, err, requireModules) {
        var e = new Error(msg + '\nhttp://requirejs.org/docs/errors.html#' + id);
        e.requireType = id;
        e.requireModules = requireModules;
        if (err) {
            e.originalError = err;
        }
        return e;
    }

    if (typeof define !== 'undefined') {
        //If a define is already in play via another AMD loader,
        //do not overwrite.
        return;
    }

    if (typeof requirejs !== 'undefined') {
        if (isFunction(requirejs)) {
            //Do not overwrite and existing requirejs instance.
            return;
        }
        cfg = requirejs;
        requirejs = undefined;
    }

    //Allow for a require config object
    if (typeof require !== 'undefined' && !isFunction(require)) {
        //assume it is a config object.
        cfg = require;
        require = undefined;
    }

    function newContext(contextName) {
        var inCheckLoaded, Module, context, handlers,
            checkLoadedTimeoutId,
            config = {
                //Defaults. Do not set a default for map
                //config to speed up normalize(), which
                //will run faster if there is no default.
                waitSeconds: 7,
                baseUrl: './',
                paths: {},
                pkgs: {},
                shim: {},
                config: {}
            },
            registry = {},
            //registry of just enabled modules, to speed
            //cycle breaking code when lots of modules
            //are registered, but not activated.
            enabledRegistry = {},
            undefEvents = {},
            defQueue = [],
            defined = {},
            urlFetched = {},
            requireCounter = 1,
            unnormalizedCounter = 1;

        /**
         * Trims the . and .. from an array of path segments.
         * It will keep a leading path segment if a .. will become
         * the first path segment, to help with module name lookups,
         * which act like paths, but can be remapped. But the end result,
         * all paths that use this function should look normalized.
         * NOTE: this method MODIFIES the input array.
         * @param {Array} ary the array of path segments.
         */
        function trimDots(ary) {
            var i, part;
            for (i = 0; ary[i]; i += 1) {
                part = ary[i];
                if (part === '.') {
                    ary.splice(i, 1);
                    i -= 1;
                } else if (part === '..') {
                    if (i === 1 && (ary[2] === '..' || ary[0] === '..')) {
                        //End of the line. Keep at least one non-dot
                        //path segment at the front so it can be mapped
                        //correctly to disk. Otherwise, there is likely
                        //no path mapping for a path starting with '..'.
                        //This can still fail, but catches the most reasonable
                        //uses of ..
                        break;
                    } else if (i > 0) {
                        ary.splice(i - 1, 2);
                        i -= 2;
                    }
                }
            }
        }

        /**
         * Given a relative module name, like ./something, normalize it to
         * a real name that can be mapped to a path.
         * @param {String} name the relative name
         * @param {String} baseName a real name that the name arg is relative
         * to.
         * @param {Boolean} applyMap apply the map config to the value. Should
         * only be done if this normalization is for a dependency ID.
         * @returns {String} normalized name
         */
        function normalize(name, baseName, applyMap) {
            var pkgName, pkgConfig, mapValue, nameParts, i, j, nameSegment,
                foundMap, foundI, foundStarMap, starI,
                baseParts = baseName && baseName.split('/'),
                normalizedBaseParts = baseParts,
                map = config.map,
                starMap = map && map['*'];

            //Adjust any relative paths.
            if (name && name.charAt(0) === '.') {
                //If have a base name, try to normalize against it,
                //otherwise, assume it is a top-level require that will
                //be relative to baseUrl in the end.
                if (baseName) {
                    if (getOwn(config.pkgs, baseName)) {
                        //If the baseName is a package name, then just treat it as one
                        //name to concat the name with.
                        normalizedBaseParts = baseParts = [baseName];
                    } else {
                        //Convert baseName to array, and lop off the last part,
                        //so that . matches that 'directory' and not name of the baseName's
                        //module. For instance, baseName of 'one/two/three', maps to
                        //'one/two/three.js', but we want the directory, 'one/two' for
                        //this normalization.
                        normalizedBaseParts = baseParts.slice(0, baseParts.length - 1);
                    }

                    name = normalizedBaseParts.concat(name.split('/'));
                    trimDots(name);

                    //Some use of packages may use a . path to reference the
                    //'main' module name, so normalize for that.
                    pkgConfig = getOwn(config.pkgs, (pkgName = name[0]));
                    name = name.join('/');
                    if (pkgConfig && name === pkgName + '/' + pkgConfig.main) {
                        name = pkgName;
                    }
                } else if (name.indexOf('./') === 0) {
                    // No baseName, so this is ID is resolved relative
                    // to baseUrl, pull off the leading dot.
                    name = name.substring(2);
                }
            }

            //Apply map config if available.
            if (applyMap && map && (baseParts || starMap)) {
                nameParts = name.split('/');

                for (i = nameParts.length; i > 0; i -= 1) {
                    nameSegment = nameParts.slice(0, i).join('/');

                    if (baseParts) {
                        //Find the longest baseName segment match in the config.
                        //So, do joins on the biggest to smallest lengths of baseParts.
                        for (j = baseParts.length; j > 0; j -= 1) {
                            mapValue = getOwn(map, baseParts.slice(0, j).join('/'));

                            //baseName segment has config, find if it has one for
                            //this name.
                            if (mapValue) {
                                mapValue = getOwn(mapValue, nameSegment);
                                if (mapValue) {
                                    //Match, update name to the new value.
                                    foundMap = mapValue;
                                    foundI = i;
                                    break;
                                }
                            }
                        }
                    }

                    if (foundMap) {
                        break;
                    }

                    //Check for a star map match, but just hold on to it,
                    //if there is a shorter segment match later in a matching
                    //config, then favor over this star map.
                    if (!foundStarMap && starMap && getOwn(starMap, nameSegment)) {
                        foundStarMap = getOwn(starMap, nameSegment);
                        starI = i;
                    }
                }

                if (!foundMap && foundStarMap) {
                    foundMap = foundStarMap;
                    foundI = starI;
                }

                if (foundMap) {
                    nameParts.splice(0, foundI, foundMap);
                    name = nameParts.join('/');
                }
            }

            return name;
        }

        function removeScript(name) {
            if (isBrowser) {
                each(scripts(), function (scriptNode) {
                    if (scriptNode.getAttribute('data-requiremodule') === name &&
                            scriptNode.getAttribute('data-requirecontext') === context.contextName) {
                        scriptNode.parentNode.removeChild(scriptNode);
                        return true;
                    }
                });
            }
        }

        function hasPathFallback(id) {
            var pathConfig = getOwn(config.paths, id);
            if (pathConfig && isArray(pathConfig) && pathConfig.length > 1) {
                removeScript(id);
                //Pop off the first array value, since it failed, and
                //retry
                pathConfig.shift();
                context.require.undef(id);
                context.require([id]);
                return true;
            }
        }

        //Turns a plugin!resource to [plugin, resource]
        //with the plugin being undefined if the name
        //did not have a plugin prefix.
        function splitPrefix(name) {
            var prefix,
                index = name ? name.indexOf('!') : -1;
            if (index > -1) {
                prefix = name.substring(0, index);
                name = name.substring(index + 1, name.length);
            }
            return [prefix, name];
        }

        /**
         * Creates a module mapping that includes plugin prefix, module
         * name, and path. If parentModuleMap is provided it will
         * also normalize the name via require.normalize()
         *
         * @param {String} name the module name
         * @param {String} [parentModuleMap] parent module map
         * for the module name, used to resolve relative names.
         * @param {Boolean} isNormalized: is the ID already normalized.
         * This is true if this call is done for a define() module ID.
         * @param {Boolean} applyMap: apply the map config to the ID.
         * Should only be true if this map is for a dependency.
         *
         * @returns {Object}
         */
        function makeModuleMap(name, parentModuleMap, isNormalized, applyMap) {
            var url, pluginModule, suffix, nameParts,
                prefix = null,
                parentName = parentModuleMap ? parentModuleMap.name : null,
                originalName = name,
                isDefine = true,
                normalizedName = '';

            //If no name, then it means it is a require call, generate an
            //internal name.
            if (!name) {
                isDefine = false;
                name = '_@r' + (requireCounter += 1);
            }

            nameParts = splitPrefix(name);
            prefix = nameParts[0];
            name = nameParts[1];

            if (prefix) {
                prefix = normalize(prefix, parentName, applyMap);
                pluginModule = getOwn(defined, prefix);
            }

            //Account for relative paths if there is a base name.
            if (name) {
                if (prefix) {
                    if (pluginModule && pluginModule.normalize) {
                        //Plugin is loaded, use its normalize method.
                        normalizedName = pluginModule.normalize(name, function (name) {
                            return normalize(name, parentName, applyMap);
                        });
                    } else {
                        normalizedName = normalize(name, parentName, applyMap);
                    }
                } else {
                    //A regular module.
                    normalizedName = normalize(name, parentName, applyMap);

                    //Normalized name may be a plugin ID due to map config
                    //application in normalize. The map config values must
                    //already be normalized, so do not need to redo that part.
                    nameParts = splitPrefix(normalizedName);
                    prefix = nameParts[0];
                    normalizedName = nameParts[1];
                    isNormalized = true;

                    url = context.nameToUrl(normalizedName);
                }
            }

            //If the id is a plugin id that cannot be determined if it needs
            //normalization, stamp it with a unique ID so two matching relative
            //ids that may conflict can be separate.
            suffix = prefix && !pluginModule && !isNormalized ?
                     '_unnormalized' + (unnormalizedCounter += 1) :
                     '';

            return {
                prefix: prefix,
                name: normalizedName,
                parentMap: parentModuleMap,
                unnormalized: !!suffix,
                url: url,
                originalName: originalName,
                isDefine: isDefine,
                id: (prefix ?
                        prefix + '!' + normalizedName :
                        normalizedName) + suffix
            };
        }

        function getModule(depMap) {
            var id = depMap.id,
                mod = getOwn(registry, id);

            if (!mod) {
                mod = registry[id] = new context.Module(depMap);
            }

            return mod;
        }

        function on(depMap, name, fn) {
            var id = depMap.id,
                mod = getOwn(registry, id);

            if (hasProp(defined, id) &&
                    (!mod || mod.defineEmitComplete)) {
                if (name === 'defined') {
                    fn(defined[id]);
                }
            } else {
                getModule(depMap).on(name, fn);
            }
        }

        function onError(err, errback) {
            var ids = err.requireModules,
                notified = false;

            if (errback) {
                errback(err);
            } else {
                each(ids, function (id) {
                    var mod = getOwn(registry, id);
                    if (mod) {
                        //Set error on module, so it skips timeout checks.
                        mod.error = err;
                        if (mod.events.error) {
                            notified = true;
                            mod.emit('error', err);
                        }
                    }
                });

                if (!notified) {
                    req.onError(err);
                }
            }
        }

        /**
         * Internal method to transfer globalQueue items to this context's
         * defQueue.
         */
        function takeGlobalQueue() {
            //Push all the globalDefQueue items into the context's defQueue
            if (globalDefQueue.length) {
                //Array splice in the values since the context code has a
                //local var ref to defQueue, so cannot just reassign the one
                //on context.
                apsp.apply(defQueue,
                           [defQueue.length - 1, 0].concat(globalDefQueue));
                globalDefQueue = [];
            }
        }

        handlers = {
            'require': function (mod) {
                if (mod.require) {
                    return mod.require;
                } else {
                    return (mod.require = context.makeRequire(mod.map));
                }
            },
            'exports': function (mod) {
                mod.usingExports = true;
                if (mod.map.isDefine) {
                    if (mod.exports) {
                        return mod.exports;
                    } else {
                        return (mod.exports = defined[mod.map.id] = {});
                    }
                }
            },
            'module': function (mod) {
                if (mod.module) {
                    return mod.module;
                } else {
                    return (mod.module = {
                        id: mod.map.id,
                        uri: mod.map.url,
                        config: function () {
                            return (config.config && getOwn(config.config, mod.map.id)) || {};
                        },
                        exports: defined[mod.map.id]
                    });
                }
            }
        };

        function cleanRegistry(id) {
            //Clean up machinery used for waiting modules.
            delete registry[id];
            delete enabledRegistry[id];
        }

        function breakCycle(mod, traced, processed) {
            var id = mod.map.id;

            if (mod.error) {
                mod.emit('error', mod.error);
            } else {
                traced[id] = true;
                each(mod.depMaps, function (depMap, i) {
                    var depId = depMap.id,
                        dep = getOwn(registry, depId);

                    //Only force things that have not completed
                    //being defined, so still in the registry,
                    //and only if it has not been matched up
                    //in the module already.
                    if (dep && !mod.depMatched[i] && !processed[depId]) {
                        if (getOwn(traced, depId)) {
                            mod.defineDep(i, defined[depId]);
                            mod.check(); //pass false?
                        } else {
                            breakCycle(dep, traced, processed);
                        }
                    }
                });
                processed[id] = true;
            }
        }

        function checkLoaded() {
            var map, modId, err, usingPathFallback,
                waitInterval = config.waitSeconds * 1000,
                //It is possible to disable the wait interval by using waitSeconds of 0.
                expired = waitInterval && (context.startTime + waitInterval) < new Date().getTime(),
                noLoads = [],
                reqCalls = [],
                stillLoading = false,
                needCycleCheck = true;

            //Do not bother if this call was a result of a cycle break.
            if (inCheckLoaded) {
                return;
            }

            inCheckLoaded = true;

            //Figure out the state of all the modules.
            eachProp(enabledRegistry, function (mod) {
                map = mod.map;
                modId = map.id;

                //Skip things that are not enabled or in error state.
                if (!mod.enabled) {
                    return;
                }

                if (!map.isDefine) {
                    reqCalls.push(mod);
                }

                if (!mod.error) {
                    //If the module should be executed, and it has not
                    //been inited and time is up, remember it.
                    if (!mod.inited && expired) {
                        if (hasPathFallback(modId)) {
                            usingPathFallback = true;
                            stillLoading = true;
                        } else {
                            noLoads.push(modId);
                            removeScript(modId);
                        }
                    } else if (!mod.inited && mod.fetched && map.isDefine) {
                        stillLoading = true;
                        if (!map.prefix) {
                            //No reason to keep looking for unfinished
                            //loading. If the only stillLoading is a
                            //plugin resource though, keep going,
                            //because it may be that a plugin resource
                            //is waiting on a non-plugin cycle.
                            return (needCycleCheck = false);
                        }
                    }
                }
            });

            if (expired && noLoads.length) {
                //If wait time expired, throw error of unloaded modules.
                err = makeError('timeout', 'Load timeout for modules: ' + noLoads, null, noLoads);
                err.contextName = context.contextName;
                return onError(err);
            }

            //Not expired, check for a cycle.
            if (needCycleCheck) {
                each(reqCalls, function (mod) {
                    breakCycle(mod, {}, {});
                });
            }

            //If still waiting on loads, and the waiting load is something
            //other than a plugin resource, or there are still outstanding
            //scripts, then just try back later.
            if ((!expired || usingPathFallback) && stillLoading) {
                //Something is still waiting to load. Wait for it, but only
                //if a timeout is not already in effect.
                if ((isBrowser || isWebWorker) && !checkLoadedTimeoutId) {
                    checkLoadedTimeoutId = setTimeout(function () {
                        checkLoadedTimeoutId = 0;
                        checkLoaded();
                    }, 50);
                }
            }

            inCheckLoaded = false;
        }

        Module = function (map) {
            this.events = getOwn(undefEvents, map.id) || {};
            this.map = map;
            this.shim = getOwn(config.shim, map.id);
            this.depExports = [];
            this.depMaps = [];
            this.depMatched = [];
            this.pluginMaps = {};
            this.depCount = 0;

            /* this.exports this.factory
               this.depMaps = [],
               this.enabled, this.fetched
            */
        };

        Module.prototype = {
            init: function (depMaps, factory, errback, options) {
                options = options || {};

                //Do not do more inits if already done. Can happen if there
                //are multiple define calls for the same module. That is not
                //a normal, common case, but it is also not unexpected.
                if (this.inited) {
                    return;
                }

                this.factory = factory;

                if (errback) {
                    //Register for errors on this module.
                    this.on('error', errback);
                } else if (this.events.error) {
                    //If no errback already, but there are error listeners
                    //on this module, set up an errback to pass to the deps.
                    errback = bind(this, function (err) {
                        this.emit('error', err);
                    });
                }

                //Do a copy of the dependency array, so that
                //source inputs are not modified. For example
                //"shim" deps are passed in here directly, and
                //doing a direct modification of the depMaps array
                //would affect that config.
                this.depMaps = depMaps && depMaps.slice(0);

                this.errback = errback;

                //Indicate this module has be initialized
                this.inited = true;

                this.ignore = options.ignore;

                //Could have option to init this module in enabled mode,
                //or could have been previously marked as enabled. However,
                //the dependencies are not known until init is called. So
                //if enabled previously, now trigger dependencies as enabled.
                if (options.enabled || this.enabled) {
                    //Enable this module and dependencies.
                    //Will call this.check()
                    this.enable();
                } else {
                    this.check();
                }
            },

            defineDep: function (i, depExports) {
                //Because of cycles, defined callback for a given
                //export can be called more than once.
                if (!this.depMatched[i]) {
                    this.depMatched[i] = true;
                    this.depCount -= 1;
                    this.depExports[i] = depExports;
                }
            },

            fetch: function () {
                if (this.fetched) {
                    return;
                }
                this.fetched = true;

                context.startTime = (new Date()).getTime();

                var map = this.map;

                //If the manager is for a plugin managed resource,
                //ask the plugin to load it now.
                if (this.shim) {
                    context.makeRequire(this.map, {
                        enableBuildCallback: true
                    })(this.shim.deps || [], bind(this, function () {
                        return map.prefix ? this.callPlugin() : this.load();
                    }));
                } else {
                    //Regular dependency.
                    return map.prefix ? this.callPlugin() : this.load();
                }
            },

            load: function () {
                var url = this.map.url;

                //Regular dependency.
                if (!urlFetched[url]) {
                    urlFetched[url] = true;
                    context.load(this.map.id, url);
                }
            },

            /**
             * Checks if the module is ready to define itself, and if so,
             * define it.
             */
            check: function () {
                if (!this.enabled || this.enabling) {
                    return;
                }

                var err, cjsModule,
                    id = this.map.id,
                    depExports = this.depExports,
                    exports = this.exports,
                    factory = this.factory;

                if (!this.inited) {
                    this.fetch();
                } else if (this.error) {
                    this.emit('error', this.error);
                } else if (!this.defining) {
                    //The factory could trigger another require call
                    //that would result in checking this module to
                    //define itself again. If already in the process
                    //of doing that, skip this work.
                    this.defining = true;

                    if (this.depCount < 1 && !this.defined) {
                        if (isFunction(factory)) {
                            //If there is an error listener, favor passing
                            //to that instead of throwing an error.
                            if (this.events.error) {
                                try {
                                    exports = context.execCb(id, factory, depExports, exports);
                                } catch (e) {
                                    err = e;
                                }
                            } else {
                                exports = context.execCb(id, factory, depExports, exports);
                            }

                            if (this.map.isDefine) {
                                //If setting exports via 'module' is in play,
                                //favor that over return value and exports. After that,
                                //favor a non-undefined return value over exports use.
                                cjsModule = this.module;
                                if (cjsModule &&
                                        cjsModule.exports !== undefined &&
                                        //Make sure it is not already the exports value
                                        cjsModule.exports !== this.exports) {
                                    exports = cjsModule.exports;
                                } else if (exports === undefined && this.usingExports) {
                                    //exports already set the defined value.
                                    exports = this.exports;
                                }
                            }

                            if (err) {
                                err.requireMap = this.map;
                                err.requireModules = [this.map.id];
                                err.requireType = 'define';
                                return onError((this.error = err));
                            }

                        } else {
                            //Just a literal value
                            exports = factory;
                        }

                        this.exports = exports;

                        if (this.map.isDefine && !this.ignore) {
                            defined[id] = exports;

                            if (req.onResourceLoad) {
                                req.onResourceLoad(context, this.map, this.depMaps);
                            }
                        }

                        //Clean up
                        cleanRegistry(id);

                        this.defined = true;
                    }

                    //Finished the define stage. Allow calling check again
                    //to allow define notifications below in the case of a
                    //cycle.
                    this.defining = false;

                    if (this.defined && !this.defineEmitted) {
                        this.defineEmitted = true;
                        this.emit('defined', this.exports);
                        this.defineEmitComplete = true;
                    }

                }
            },

            callPlugin: function () {
                var map = this.map,
                    id = map.id,
                    //Map already normalized the prefix.
                    pluginMap = makeModuleMap(map.prefix);

                //Mark this as a dependency for this plugin, so it
                //can be traced for cycles.
                this.depMaps.push(pluginMap);

                on(pluginMap, 'defined', bind(this, function (plugin) {
                    var load, normalizedMap, normalizedMod,
                        name = this.map.name,
                        parentName = this.map.parentMap ? this.map.parentMap.name : null,
                        localRequire = context.makeRequire(map.parentMap, {
                            enableBuildCallback: true
                        });

                    //If current map is not normalized, wait for that
                    //normalized name to load instead of continuing.
                    if (this.map.unnormalized) {
                        //Normalize the ID if the plugin allows it.
                        if (plugin.normalize) {
                            name = plugin.normalize(name, function (name) {
                                return normalize(name, parentName, true);
                            }) || '';
                        }

                        //prefix and name should already be normalized, no need
                        //for applying map config again either.
                        normalizedMap = makeModuleMap(map.prefix + '!' + name,
                                                      this.map.parentMap);
                        on(normalizedMap,
                            'defined', bind(this, function (value) {
                                this.init([], function () { return value; }, null, {
                                    enabled: true,
                                    ignore: true
                                });
                            }));

                        normalizedMod = getOwn(registry, normalizedMap.id);
                        if (normalizedMod) {
                            //Mark this as a dependency for this plugin, so it
                            //can be traced for cycles.
                            this.depMaps.push(normalizedMap);

                            if (this.events.error) {
                                normalizedMod.on('error', bind(this, function (err) {
                                    this.emit('error', err);
                                }));
                            }
                            normalizedMod.enable();
                        }

                        return;
                    }

                    load = bind(this, function (value) {
                        this.init([], function () { return value; }, null, {
                            enabled: true
                        });
                    });

                    load.error = bind(this, function (err) {
                        this.inited = true;
                        this.error = err;
                        err.requireModules = [id];

                        //Remove temp unnormalized modules for this module,
                        //since they will never be resolved otherwise now.
                        eachProp(registry, function (mod) {
                            if (mod.map.id.indexOf(id + '_unnormalized') === 0) {
                                cleanRegistry(mod.map.id);
                            }
                        });

                        onError(err);
                    });

                    //Allow plugins to load other code without having to know the
                    //context or how to 'complete' the load.
                    load.fromText = bind(this, function (text, textAlt) {
                        /*jslint evil: true */
                        var moduleName = map.name,
                            moduleMap = makeModuleMap(moduleName),
                            hasInteractive = useInteractive;

                        //As of 2.1.0, support just passing the text, to reinforce
                        //fromText only being called once per resource. Still
                        //support old style of passing moduleName but discard
                        //that moduleName in favor of the internal ref.
                        if (textAlt) {
                            text = textAlt;
                        }

                        //Turn off interactive script matching for IE for any define
                        //calls in the text, then turn it back on at the end.
                        if (hasInteractive) {
                            useInteractive = false;
                        }

                        //Prime the system by creating a module instance for
                        //it.
                        getModule(moduleMap);

                        //Transfer any config to this other module.
                        if (hasProp(config.config, id)) {
                            config.config[moduleName] = config.config[id];
                        }

                        try {
                            req.exec(text);
                        } catch (e) {
                            return onError(makeError('fromtexteval',
                                             'fromText eval for ' + id +
                                            ' failed: ' + e,
                                             e,
                                             [id]));
                        }

                        if (hasInteractive) {
                            useInteractive = true;
                        }

                        //Mark this as a dependency for the plugin
                        //resource
                        this.depMaps.push(moduleMap);

                        //Support anonymous modules.
                        context.completeLoad(moduleName);

                        //Bind the value of that module to the value for this
                        //resource ID.
                        localRequire([moduleName], load);
                    });

                    //Use parentName here since the plugin's name is not reliable,
                    //could be some weird string with no path that actually wants to
                    //reference the parentName's path.
                    plugin.load(map.name, localRequire, load, config);
                }));

                context.enable(pluginMap, this);
                this.pluginMaps[pluginMap.id] = pluginMap;
            },

            enable: function () {
                enabledRegistry[this.map.id] = this;
                this.enabled = true;

                //Set flag mentioning that the module is enabling,
                //so that immediate calls to the defined callbacks
                //for dependencies do not trigger inadvertent load
                //with the depCount still being zero.
                this.enabling = true;

                //Enable each dependency
                each(this.depMaps, bind(this, function (depMap, i) {
                    var id, mod, handler;

                    if (typeof depMap === 'string') {
                        //Dependency needs to be converted to a depMap
                        //and wired up to this module.
                        depMap = makeModuleMap(depMap,
                                               (this.map.isDefine ? this.map : this.map.parentMap),
                                               false,
                                               !this.skipMap);
                        this.depMaps[i] = depMap;

                        handler = getOwn(handlers, depMap.id);

                        if (handler) {
                            this.depExports[i] = handler(this);
                            return;
                        }

                        this.depCount += 1;

                        on(depMap, 'defined', bind(this, function (depExports) {
                            this.defineDep(i, depExports);
                            this.check();
                        }));

                        if (this.errback) {
                            on(depMap, 'error', this.errback);
                        }
                    }

                    id = depMap.id;
                    mod = registry[id];

                    //Skip special modules like 'require', 'exports', 'module'
                    //Also, don't call enable if it is already enabled,
                    //important in circular dependency cases.
                    if (!hasProp(handlers, id) && mod && !mod.enabled) {
                        context.enable(depMap, this);
                    }
                }));

                //Enable each plugin that is used in
                //a dependency
                eachProp(this.pluginMaps, bind(this, function (pluginMap) {
                    var mod = getOwn(registry, pluginMap.id);
                    if (mod && !mod.enabled) {
                        context.enable(pluginMap, this);
                    }
                }));

                this.enabling = false;

                this.check();
            },

            on: function (name, cb) {
                var cbs = this.events[name];
                if (!cbs) {
                    cbs = this.events[name] = [];
                }
                cbs.push(cb);
            },

            emit: function (name, evt) {
                each(this.events[name], function (cb) {
                    cb(evt);
                });
                if (name === 'error') {
                    //Now that the error handler was triggered, remove
                    //the listeners, since this broken Module instance
                    //can stay around for a while in the registry.
                    delete this.events[name];
                }
            }
        };

        function callGetModule(args) {
            //Skip modules already defined.
            if (!hasProp(defined, args[0])) {
                getModule(makeModuleMap(args[0], null, true)).init(args[1], args[2]);
            }
        }

        function removeListener(node, func, name, ieName) {
            //Favor detachEvent because of IE9
            //issue, see attachEvent/addEventListener comment elsewhere
            //in this file.
            if (node.detachEvent && !isOpera) {
                //Probably IE. If not it will throw an error, which will be
                //useful to know.
                if (ieName) {
                    node.detachEvent(ieName, func);
                }
            } else {
                node.removeEventListener(name, func, false);
            }
        }

        /**
         * Given an event from a script node, get the requirejs info from it,
         * and then removes the event listeners on the node.
         * @param {Event} evt
         * @returns {Object}
         */
        function getScriptData(evt) {
            //Using currentTarget instead of target for Firefox 2.0's sake. Not
            //all old browsers will be supported, but this one was easy enough
            //to support and still makes sense.
            var node = evt.currentTarget || evt.srcElement;

            //Remove the listeners once here.
            removeListener(node, context.onScriptLoad, 'load', 'onreadystatechange');
            removeListener(node, context.onScriptError, 'error');

            return {
                node: node,
                id: node && node.getAttribute('data-requiremodule')
            };
        }

        function intakeDefines() {
            var args;

            //Any defined modules in the global queue, intake them now.
            takeGlobalQueue();

            //Make sure any remaining defQueue items get properly processed.
            while (defQueue.length) {
                args = defQueue.shift();
                if (args[0] === null) {
                    return onError(makeError('mismatch', 'Mismatched anonymous define() module: ' + args[args.length - 1]));
                } else {
                    //args are id, deps, factory. Should be normalized by the
                    //define() function.
                    callGetModule(args);
                }
            }
        }

        context = {
            config: config,
            contextName: contextName,
            registry: registry,
            defined: defined,
            urlFetched: urlFetched,
            defQueue: defQueue,
            Module: Module,
            makeModuleMap: makeModuleMap,
            nextTick: req.nextTick,
            onError: onError,

            /**
             * Set a configuration for the context.
             * @param {Object} cfg config object to integrate.
             */
            configure: function (cfg) {
                //Make sure the baseUrl ends in a slash.
                if (cfg.baseUrl) {
                    if (cfg.baseUrl.charAt(cfg.baseUrl.length - 1) !== '/') {
                        cfg.baseUrl += '/';
                    }
                }

                //Save off the paths and packages since they require special processing,
                //they are additive.
                var pkgs = config.pkgs,
                    shim = config.shim,
                    objs = {
                        paths: true,
                        config: true,
                        map: true
                    };

                eachProp(cfg, function (value, prop) {
                    if (objs[prop]) {
                        if (prop === 'map') {
                            if (!config.map) {
                                config.map = {};
                            }
                            mixin(config[prop], value, true, true);
                        } else {
                            mixin(config[prop], value, true);
                        }
                    } else {
                        config[prop] = value;
                    }
                });

                //Merge shim
                if (cfg.shim) {
                    eachProp(cfg.shim, function (value, id) {
                        //Normalize the structure
                        if (isArray(value)) {
                            value = {
                                deps: value
                            };
                        }
                        if ((value.exports || value.init) && !value.exportsFn) {
                            value.exportsFn = context.makeShimExports(value);
                        }
                        shim[id] = value;
                    });
                    config.shim = shim;
                }

                //Adjust packages if necessary.
                if (cfg.packages) {
                    each(cfg.packages, function (pkgObj) {
                        var location;

                        pkgObj = typeof pkgObj === 'string' ? { name: pkgObj } : pkgObj;
                        location = pkgObj.location;

                        //Create a brand new object on pkgs, since currentPackages can
                        //be passed in again, and config.pkgs is the internal transformed
                        //state for all package configs.
                        pkgs[pkgObj.name] = {
                            name: pkgObj.name,
                            location: location || pkgObj.name,
                            //Remove leading dot in main, so main paths are normalized,
                            //and remove any trailing .js, since different package
                            //envs have different conventions: some use a module name,
                            //some use a file name.
                            main: (pkgObj.main || 'main')
                                  .replace(currDirRegExp, '')
                                  .replace(jsSuffixRegExp, '')
                        };
                    });

                    //Done with modifications, assing packages back to context config
                    config.pkgs = pkgs;
                }

                //If there are any "waiting to execute" modules in the registry,
                //update the maps for them, since their info, like URLs to load,
                //may have changed.
                eachProp(registry, function (mod, id) {
                    //If module already has init called, since it is too
                    //late to modify them, and ignore unnormalized ones
                    //since they are transient.
                    if (!mod.inited && !mod.map.unnormalized) {
                        mod.map = makeModuleMap(id);
                    }
                });

                //If a deps array or a config callback is specified, then call
                //require with those args. This is useful when require is defined as a
                //config object before require.js is loaded.
                if (cfg.deps || cfg.callback) {
                    context.require(cfg.deps || [], cfg.callback);
                }
            },

            makeShimExports: function (value) {
                function fn() {
                    var ret;
                    if (value.init) {
                        ret = value.init.apply(global, arguments);
                    }
                    return ret || (value.exports && getGlobal(value.exports));
                }
                return fn;
            },

            makeRequire: function (relMap, options) {
                options = options || {};

                function localRequire(deps, callback, errback) {
                    var id, map, requireMod;

                    if (options.enableBuildCallback && callback && isFunction(callback)) {
                        callback.__requireJsBuild = true;
                    }

                    if (typeof deps === 'string') {
                        if (isFunction(callback)) {
                            //Invalid call
                            return onError(makeError('requireargs', 'Invalid require call'), errback);
                        }

                        //If require|exports|module are requested, get the
                        //value for them from the special handlers. Caveat:
                        //this only works while module is being defined.
                        if (relMap && hasProp(handlers, deps)) {
                            return handlers[deps](registry[relMap.id]);
                        }

                        //Synchronous access to one module. If require.get is
                        //available (as in the Node adapter), prefer that.
                        if (req.get) {
                            return req.get(context, deps, relMap, localRequire);
                        }

                        //Normalize module name, if it contains . or ..
                        map = makeModuleMap(deps, relMap, false, true);
                        id = map.id;

                        if (!hasProp(defined, id)) {
                            return onError(makeError('notloaded', 'Module name "' +
                                        id +
                                        '" has not been loaded yet for context: ' +
                                        contextName +
                                        (relMap ? '' : '. Use require([])')));
                        }
                        return defined[id];
                    }

                    //Grab defines waiting in the global queue.
                    intakeDefines();

                    //Mark all the dependencies as needing to be loaded.
                    context.nextTick(function () {
                        //Some defines could have been added since the
                        //require call, collect them.
                        intakeDefines();

                        requireMod = getModule(makeModuleMap(null, relMap));

                        //Store if map config should be applied to this require
                        //call for dependencies.
                        requireMod.skipMap = options.skipMap;

                        requireMod.init(deps, callback, errback, {
                            enabled: true
                        });

                        checkLoaded();
                    });

                    return localRequire;
                }

                mixin(localRequire, {
                    isBrowser: isBrowser,

                    /**
                     * Converts a module name + .extension into an URL path.
                     * *Requires* the use of a module name. It does not support using
                     * plain URLs like nameToUrl.
                     */
                    toUrl: function (moduleNamePlusExt) {
                        var ext,
                            index = moduleNamePlusExt.lastIndexOf('.'),
                            segment = moduleNamePlusExt.split('/')[0],
                            isRelative = segment === '.' || segment === '..';

                        //Have a file extension alias, and it is not the
                        //dots from a relative path.
                        if (index !== -1 && (!isRelative || index > 1)) {
                            ext = moduleNamePlusExt.substring(index, moduleNamePlusExt.length);
                            moduleNamePlusExt = moduleNamePlusExt.substring(0, index);
                        }

                        return context.nameToUrl(normalize(moduleNamePlusExt,
                                                relMap && relMap.id, true), ext,  true);
                    },

                    defined: function (id) {
                        return hasProp(defined, makeModuleMap(id, relMap, false, true).id);
                    },

                    specified: function (id) {
                        id = makeModuleMap(id, relMap, false, true).id;
                        return hasProp(defined, id) || hasProp(registry, id);
                    }
                });

                //Only allow undef on top level require calls
                if (!relMap) {
                    localRequire.undef = function (id) {
                        //Bind any waiting define() calls to this context,
                        //fix for #408
                        takeGlobalQueue();

                        var map = makeModuleMap(id, relMap, true),
                            mod = getOwn(registry, id);

                        delete defined[id];
                        delete urlFetched[map.url];
                        delete undefEvents[id];

                        if (mod) {
                            //Hold on to listeners in case the
                            //module will be attempted to be reloaded
                            //using a different config.
                            if (mod.events.defined) {
                                undefEvents[id] = mod.events;
                            }

                            cleanRegistry(id);
                        }
                    };
                }

                return localRequire;
            },

            /**
             * Called to enable a module if it is still in the registry
             * awaiting enablement. A second arg, parent, the parent module,
             * is passed in for context, when this method is overriden by
             * the optimizer. Not shown here to keep code compact.
             */
            enable: function (depMap) {
                var mod = getOwn(registry, depMap.id);
                if (mod) {
                    getModule(depMap).enable();
                }
            },

            /**
             * Internal method used by environment adapters to complete a load event.
             * A load event could be a script load or just a load pass from a synchronous
             * load call.
             * @param {String} moduleName the name of the module to potentially complete.
             */
            completeLoad: function (moduleName) {
                var found, args, mod,
                    shim = getOwn(config.shim, moduleName) || {},
                    shExports = shim.exports;

                takeGlobalQueue();

                while (defQueue.length) {
                    args = defQueue.shift();
                    if (args[0] === null) {
                        args[0] = moduleName;
                        //If already found an anonymous module and bound it
                        //to this name, then this is some other anon module
                        //waiting for its completeLoad to fire.
                        if (found) {
                            break;
                        }
                        found = true;
                    } else if (args[0] === moduleName) {
                        //Found matching define call for this script!
                        found = true;
                    }

                    callGetModule(args);
                }

                //Do this after the cycle of callGetModule in case the result
                //of those calls/init calls changes the registry.
                mod = getOwn(registry, moduleName);

                if (!found && !hasProp(defined, moduleName) && mod && !mod.inited) {
                    if (config.enforceDefine && (!shExports || !getGlobal(shExports))) {
                        if (hasPathFallback(moduleName)) {
                            return;
                        } else {
                            return onError(makeError('nodefine',
                                             'No define call for ' + moduleName,
                                             null,
                                             [moduleName]));
                        }
                    } else {
                        //A script that does not call define(), so just simulate
                        //the call for it.
                        callGetModule([moduleName, (shim.deps || []), shim.exportsFn]);
                    }
                }

                checkLoaded();
            },

            /**
             * Converts a module name to a file path. Supports cases where
             * moduleName may actually be just an URL.
             * Note that it **does not** call normalize on the moduleName,
             * it is assumed to have already been normalized. This is an
             * internal API, not a public one. Use toUrl for the public API.
             */
            nameToUrl: function (moduleName, ext, skipExt) {
                var paths, pkgs, pkg, pkgPath, syms, i, parentModule, url,
                    parentPath;

                //If a colon is in the URL, it indicates a protocol is used and it is just
                //an URL to a file, or if it starts with a slash, contains a query arg (i.e. ?)
                //or ends with .js, then assume the user meant to use an url and not a module id.
                //The slash is important for protocol-less URLs as well as full paths.
                if (req.jsExtRegExp.test(moduleName)) {
                    //Just a plain path, not module name lookup, so just return it.
                    //Add extension if it is included. This is a bit wonky, only non-.js things pass
                    //an extension, this method probably needs to be reworked.
                    url = moduleName + (ext || '');
                } else {
                    //A module that needs to be converted to a path.
                    paths = config.paths;
                    pkgs = config.pkgs;

                    syms = moduleName.split('/');
                    //For each module name segment, see if there is a path
                    //registered for it. Start with most specific name
                    //and work up from it.
                    for (i = syms.length; i > 0; i -= 1) {
                        parentModule = syms.slice(0, i).join('/');
                        pkg = getOwn(pkgs, parentModule);
                        parentPath = getOwn(paths, parentModule);
                        if (parentPath) {
                            //If an array, it means there are a few choices,
                            //Choose the one that is desired
                            if (isArray(parentPath)) {
                                parentPath = parentPath[0];
                            }
                            syms.splice(0, i, parentPath);
                            break;
                        } else if (pkg) {
                            //If module name is just the package name, then looking
                            //for the main module.
                            if (moduleName === pkg.name) {
                                pkgPath = pkg.location + '/' + pkg.main;
                            } else {
                                pkgPath = pkg.location;
                            }
                            syms.splice(0, i, pkgPath);
                            break;
                        }
                    }

                    //Join the path parts together, then figure out if baseUrl is needed.
                    url = syms.join('/');
                    url += (ext || (/\?/.test(url) || skipExt ? '' : '.js'));
                    url = (url.charAt(0) === '/' || url.match(/^[\w\+\.\-]+:/) ? '' : config.baseUrl) + url;
                }

                return config.urlArgs ? url +
                                        ((url.indexOf('?') === -1 ? '?' : '&') +
                                         config.urlArgs) : url;
            },

            //Delegates to req.load. Broken out as a separate function to
            //allow overriding in the optimizer.
            load: function (id, url) {
                req.load(context, id, url);
            },

            /**
             * Executes a module callback function. Broken out as a separate function
             * solely to allow the build system to sequence the files in the built
             * layer in the right sequence.
             *
             * @private
             */
            execCb: function (name, callback, args, exports) {
                return callback.apply(exports, args);
            },

            /**
             * callback for script loads, used to check status of loading.
             *
             * @param {Event} evt the event from the browser for the script
             * that was loaded.
             */
            onScriptLoad: function (evt) {
                //Using currentTarget instead of target for Firefox 2.0's sake. Not
                //all old browsers will be supported, but this one was easy enough
                //to support and still makes sense.
                if (evt.type === 'load' ||
                        (readyRegExp.test((evt.currentTarget || evt.srcElement).readyState))) {
                    //Reset interactive script so a script node is not held onto for
                    //to long.
                    interactiveScript = null;

                    //Pull out the name of the module and the context.
                    var data = getScriptData(evt);
                    context.completeLoad(data.id);
                }
            },

            /**
             * Callback for script errors.
             */
            onScriptError: function (evt) {
                var data = getScriptData(evt);
                if (!hasPathFallback(data.id)) {
                    return onError(makeError('scripterror', 'Script error', evt, [data.id]));
                }
            }
        };

        context.require = context.makeRequire();
        return context;
    }

    /**
     * Main entry point.
     *
     * If the only argument to require is a string, then the module that
     * is represented by that string is fetched for the appropriate context.
     *
     * If the first argument is an array, then it will be treated as an array
     * of dependency string names to fetch. An optional function callback can
     * be specified to execute when all of those dependencies are available.
     *
     * Make a local req variable to help Caja compliance (it assumes things
     * on a require that are not standardized), and to give a short
     * name for minification/local scope use.
     */
    req = requirejs = function (deps, callback, errback, optional) {

        //Find the right context, use default
        var context, config,
            contextName = defContextName;

        // Determine if have config object in the call.
        if (!isArray(deps) && typeof deps !== 'string') {
            // deps is a config object
            config = deps;
            if (isArray(callback)) {
                // Adjust args if there are dependencies
                deps = callback;
                callback = errback;
                errback = optional;
            } else {
                deps = [];
            }
        }

        if (config && config.context) {
            contextName = config.context;
        }

        context = getOwn(contexts, contextName);
        if (!context) {
            context = contexts[contextName] = req.s.newContext(contextName);
        }

        if (config) {
            context.configure(config);
        }

        return context.require(deps, callback, errback);
    };

    /**
     * Support require.config() to make it easier to cooperate with other
     * AMD loaders on globally agreed names.
     */
    req.config = function (config) {
        return req(config);
    };

    /**
     * Execute something after the current tick
     * of the event loop. Override for other envs
     * that have a better solution than setTimeout.
     * @param  {Function} fn function to execute later.
     */
    req.nextTick = typeof setTimeout !== 'undefined' ? function (fn) {
        setTimeout(fn, 4);
    } : function (fn) { fn(); };

    /**
     * Export require as a global, but only if it does not already exist.
     */
    if (!require) {
        require = req;
    }

    req.version = version;

    //Used to filter out dependencies that are already paths.
    req.jsExtRegExp = /^\/|:|\?|\.js$/;
    req.isBrowser = isBrowser;
    s = req.s = {
        contexts: contexts,
        newContext: newContext
    };

    //Create default context.
    req({});

    //Exports some context-sensitive methods on global require.
    each([
        'toUrl',
        'undef',
        'defined',
        'specified'
    ], function (prop) {
        //Reference from contexts instead of early binding to default context,
        //so that during builds, the latest instance of the default context
        //with its config gets used.
        req[prop] = function () {
            var ctx = contexts[defContextName];
            return ctx.require[prop].apply(ctx, arguments);
        };
    });

    if (isBrowser) {
        head = s.head = document.getElementsByTagName('head')[0];
        //If BASE tag is in play, using appendChild is a problem for IE6.
        //When that browser dies, this can be removed. Details in this jQuery bug:
        //http://dev.jquery.com/ticket/2709
        baseElement = document.getElementsByTagName('base')[0];
        if (baseElement) {
            head = s.head = baseElement.parentNode;
        }
    }

    /**
     * Any errors that require explicitly generates will be passed to this
     * function. Intercept/override it if you want custom error handling.
     * @param {Error} err the error object.
     */
    req.onError = function (err) {
        throw err;
    };

    /**
     * Does the request to load a module for the browser case.
     * Make this a separate function to allow other environments
     * to override it.
     *
     * @param {Object} context the require context to find state.
     * @param {String} moduleName the name of the module.
     * @param {Object} url the URL to the module.
     */
    req.load = function (context, moduleName, url) {
        var config = (context && context.config) || {},
            node;
        if (isBrowser) {
            //In the browser so use a script tag
            node = config.xhtml ?
                    document.createElementNS('http://www.w3.org/1999/xhtml', 'html:script') :
                    document.createElement('script');
            node.type = config.scriptType || 'text/javascript';
            node.charset = 'utf-8';
            node.async = true;

            node.setAttribute('data-requirecontext', context.contextName);
            node.setAttribute('data-requiremodule', moduleName);

            //Set up load listener. Test attachEvent first because IE9 has
            //a subtle issue in its addEventListener and script onload firings
            //that do not match the behavior of all other browsers with
            //addEventListener support, which fire the onload event for a
            //script right after the script execution. See:
            //https://connect.microsoft.com/IE/feedback/details/648057/script-onload-event-is-not-fired-immediately-after-script-execution
            //UNFORTUNATELY Opera implements attachEvent but does not follow the script
            //script execution mode.
            if (node.attachEvent &&
                    //Check if node.attachEvent is artificially added by custom script or
                    //natively supported by browser
                    //read https://github.com/jrburke/requirejs/issues/187
                    //if we can NOT find [native code] then it must NOT natively supported.
                    //in IE8, node.attachEvent does not have toString()
                    //Note the test for "[native code" with no closing brace, see:
                    //https://github.com/jrburke/requirejs/issues/273
                    !(node.attachEvent.toString && node.attachEvent.toString().indexOf('[native code') < 0) &&
                    !isOpera) {
                //Probably IE. IE (at least 6-8) do not fire
                //script onload right after executing the script, so
                //we cannot tie the anonymous define call to a name.
                //However, IE reports the script as being in 'interactive'
                //readyState at the time of the define call.
                useInteractive = true;

                node.attachEvent('onreadystatechange', context.onScriptLoad);
                //It would be great to add an error handler here to catch
                //404s in IE9+. However, onreadystatechange will fire before
                //the error handler, so that does not help. If addEventListener
                //is used, then IE will fire error before load, but we cannot
                //use that pathway given the connect.microsoft.com issue
                //mentioned above about not doing the 'script execute,
                //then fire the script load event listener before execute
                //next script' that other browsers do.
                //Best hope: IE10 fixes the issues,
                //and then destroys all installs of IE 6-9.
                //node.attachEvent('onerror', context.onScriptError);
            } else {
                node.addEventListener('load', context.onScriptLoad, false);
                node.addEventListener('error', context.onScriptError, false);
            }
            node.src = url;

            //For some cache cases in IE 6-8, the script executes before the end
            //of the appendChild execution, so to tie an anonymous define
            //call to the module name (which is stored on the node), hold on
            //to a reference to this node, but clear after the DOM insertion.
            currentlyAddingScript = node;
            if (baseElement) {
                head.insertBefore(node, baseElement);
            } else {
                head.appendChild(node);
            }
            currentlyAddingScript = null;

            return node;
        } else if (isWebWorker) {
            try {
                //In a web worker, use importScripts. This is not a very
                //efficient use of importScripts, importScripts will block until
                //its script is downloaded and evaluated. However, if web workers
                //are in play, the expectation that a build has been done so that
                //only one script needs to be loaded anyway. This may need to be
                //reevaluated if other use cases become common.
                importScripts(url);

                //Account for anonymous modules
                context.completeLoad(moduleName);
            } catch (e) {
                context.onError(makeError('importscripts',
                                'importScripts failed for ' +
                                    moduleName + ' at ' + url,
                                e,
                                [moduleName]));
            }
        }
    };

    function getInteractiveScript() {
        if (interactiveScript && interactiveScript.readyState === 'interactive') {
            return interactiveScript;
        }

        eachReverse(scripts(), function (script) {
            if (script.readyState === 'interactive') {
                return (interactiveScript = script);
            }
        });
        return interactiveScript;
    }

    //Look for a data-main script attribute, which could also adjust the baseUrl.
    if (isBrowser) {
        //Figure out baseUrl. Get it from the script tag with require.js in it.
        eachReverse(scripts(), function (script) {
            //Set the 'head' where we can append children by
            //using the script's parent.
            if (!head) {
                head = script.parentNode;
            }

            //Look for a data-main attribute to set main script for the page
            //to load. If it is there, the path to data main becomes the
            //baseUrl, if it is not already set.
            dataMain = script.getAttribute('data-main');
            if (dataMain) {
                //Set final baseUrl if there is not already an explicit one.
                if (!cfg.baseUrl) {
                    //Pull off the directory of data-main for use as the
                    //baseUrl.
                    src = dataMain.split('/');
                    mainScript = src.pop();
                    subPath = src.length ? src.join('/')  + '/' : './';

                    cfg.baseUrl = subPath;
                    dataMain = mainScript;
                }

                //Strip off any trailing .js since dataMain is now
                //like a module name.
                dataMain = dataMain.replace(jsSuffixRegExp, '');

                //Put the data-main script in the files to load.
                cfg.deps = cfg.deps ? cfg.deps.concat(dataMain) : [dataMain];

                return true;
            }
        });
    }

    /**
     * The function that handles definitions of modules. Differs from
     * require() in that a string for the module should be the first argument,
     * and the function to execute after dependencies are loaded should
     * return a value to define the module corresponding to the first argument's
     * name.
     */
    define = function (name, deps, callback) {
        var node, context;

        //Allow for anonymous modules
        if (typeof name !== 'string') {
            //Adjust args appropriately
            callback = deps;
            deps = name;
            name = null;
        }

        //This module may not have dependencies
        if (!isArray(deps)) {
            callback = deps;
            deps = null;
        }

        //If no name, and callback is a function, then figure out if it a
        //CommonJS thing with dependencies.
        if (!deps && isFunction(callback)) {
            deps = [];
            //Remove comments from the callback string,
            //look for require calls, and pull them into the dependencies,
            //but only if there are function args.
            if (callback.length) {
                callback
                    .toString()
                    .replace(commentRegExp, '')
                    .replace(cjsRequireRegExp, function (match, dep) {
                        deps.push(dep);
                    });

                //May be a CommonJS thing even without require calls, but still
                //could use exports, and module. Avoid doing exports and module
                //work though if it just needs require.
                //REQUIRES the function to expect the CommonJS variables in the
                //order listed below.
                deps = (callback.length === 1 ? ['require'] : ['require', 'exports', 'module']).concat(deps);
            }
        }

        //If in IE 6-8 and hit an anonymous define() call, do the interactive
        //work.
        if (useInteractive) {
            node = currentlyAddingScript || getInteractiveScript();
            if (node) {
                if (!name) {
                    name = node.getAttribute('data-requiremodule');
                }
                context = contexts[node.getAttribute('data-requirecontext')];
            }
        }

        //Always save off evaluating the def call until the script onload handler.
        //This allows multiple modules to be in a file without prematurely
        //tracing dependencies, and allows for anonymous module support,
        //where the module name is not known until the script onload event
        //occurs. If no context, use the global queue, and get it processed
        //in the onscript load callback.
        (context ? context.defQueue : globalDefQueue).push([name, deps, callback]);
    };

    define.amd = {
        jQuery: true
    };


    /**
     * Executes the text. Normally just uses eval, but can be modified
     * to use a better, environment-specific call. Only used for transpiling
     * loader plugins, not for plain JS modules.
     * @param {String} text the text to execute/evaluate.
     */
    req.exec = function (text) {
        /*jslint evil: true */
        return eval(text);
    };

    //Set up with config info.
    req(cfg);
}(this));

;(function(win){
	var hrefStr = win["location"].href||document.URL||""
	CData = function CData(){
		_CData.log('CData constructor')
		var ins =  new _CData();
		var ret = {};
		for(var n in ins){
			if(n[0]!='_'&&!ins.hasOwnProperty(n)){
				if (typeof ins[n] == 'function'){
					ret[n] = ins[n].bind(ins);
				}
			}
		}
		
		_CData.log(ret.setData)
		return ret
	}
	CData.queue = {};
	CData.readyQueue = [];
	CData.no = 0;
	CData.debug = true;
	function _CData(){
		this.DATAS = {};
		//记录实时数据
		this.REALTIMEDATAS = {};
		this.shows = [];
		this.datas = [];
		this.methods = {};
		//define database type
		this.DATATYPE="";
		this.URLS = null;
		
		this.asyn = 0;
		this.ASYNS = [];
		var no = (++CData.no)
		this.time = +(new Date);
		this.time = this.time+"_"+no;
		
		CData.queue[this.time] = {
			"href":hrefStr,
			time:this.time,
			no:no,
			fns:[],
			complete:0,
			constructor:this
		}
		//console.log(this.time,no)
	}
	_CData.log=function(name,msg){
		if (window.console&&console.info){
			if(!CData.debug)return;
			if(navigator.userAgent.indexOf("Firefox")==-1)return;
			var arr=Array.prototype.slice.call(arguments);
			if (console.info.apply){
				console.info.apply(console,arr);
			}else{
				console.info(arr.join(' | '));
			}
		}
	};
	CData.log = _CData.log;
	CData.formatData = function(data,type){
		var isArray = function(a){
			return a &&
			typeof a === 'object' &&
			typeof a.length === 'number' &&
			typeof a.splice === 'function' &&
			!(a.propertyIsEnumerable('length'));
		};
		var data = data;
		var type = type;
		var self = this;
		switch(type){
			default:
				return def(data,type);
			break;
		}
		function def(data,type){
		
			if(!data.format){
				CData.log(data.format)
				var ret = {
					categories:[],
					series:[]
				};
				return ret;
			}
			var format = data.format;
			var categories = [];
			for(var i=0;i<format.length;i++){
				var item = format[i];
				if(isArray(item)){
					categories = item;
					break;
				}
			}
			var results = data.results;
			
			var series = [];
			for(var i=0;i<results.length;i++){
				var  result = results[i];
				var  lists = result["data-lists"];
				var name =  result.name||"";//"name_"+i;
				for(var j=0;j<lists.length;j++){
					var list = lists[j];
					var data = [];
					var obj = {
						name: name,
						data: []
					}
					for(var k=0;k<list.length;k++){
						var item = list[k];
						
						if(isArray(item)){
							obj.data = item;
							break;
						}else{
							obj.name +=" "+item;
						}
					}
					// if(j==0)
					series.push(obj)
				}
			}
			var ret = {
				categories:categories,
				series:series
			};
			
			return ret;
		}
	}
	CData.combo = function(loops,success,port){
		var url = "http://"+IP+":"+(port?port:9999)+"/combo?callback=?";
		jQuery.ajax({
			"cache":true,
			url: url,
			data: {
				"loops":loops
			},
			dataType: "json",
			error: function(e){
				CData.log('error',e);
			}, 
			success:success||$.noop
		});
	},
	CData.sendmail = function(mailOptions,callback){
		var url = "http://"+IP+":"+sendmailPort+"/sendmail?callback=?";
		jQuery.ajax({
			//"cache":true,
			url: url,
			data: {
				"mailOptions":mailOptions
			},
			dataType: "json",
			error: function(e){
				CData.log('error',e);
			}, 
			success:callback||$.noop
		});
	}
	CData.feedmail = {};
	CData.feedmail.add = function(opt,callback){
		var url = "http://"+IP+":"+feedmailPort+"/feedmail/add?callback=?";
		jQuery.ajax({
			url: url,
			data: {
				"options":opt
			},
			dataType: "json",
			error: function(e){
				CData.log('error',e);
			}, 
			success:callback||$.noop
		});
	},
	CData.feedmail.del = function(opt,callback){
		var url = "http://"+IP+":"+feedmailPort+"/feedmail/del?callback=?";
		jQuery.ajax({
			//"cache":true,
			url: url,
			data: {
				"options":opt
			},
			dataType: "json",
			error: function(e){
				CData.log('error',e);
			}, 
			success:callback||$.noop
		});
	},
	CData.toCSV = function(data){
		function post(data){
			var HIDDEN = 'hidden',
				NONE = 'none',
				DIV = 'div',
				garbageBin;
			var doc = window.document;
			function extend(a, b) {
				var n;
				if (!a) {
					a = {};
				}
				for (n in b) {
					a[n] = b[n];
				}
				return a;
			}
			function css(el, styles) {
				extend(el.style, styles);
			}
			function createElement(tag, attribs, styles, parent, nopad) {
				var el = doc.createElement(tag);
				if (attribs) {
					extend(el, attribs);
				}
				if (nopad) {
					css(el, {padding: 0, border: NONE, margin: 0});
				}
				if (styles) {
					css(el, styles);
				}
				if (parent) {
					parent.appendChild(el);
				}
				return el;
			}
			function discardElement(element) {
				// create a garbage bin element, not part of the DOM
				if (!garbageBin) {
					garbageBin = createElement(DIV);
				}

				// move the node and empty bin
				if (element) {
					garbageBin.appendChild(element);
				}
				garbageBin.innerHTML = '';
			}
			var name,
			form;
			
			// create the form
			form = createElement('form', {
				method: 'post',
				action: "http://"+IP+":"+toCSVPort+"/toCSV",
				target:"_blank"
				// ,enctype: 'multipart/form-data' 
			}, {
				display: NONE
			}, doc.body);

			// add the data
			for (name in data) {
				createElement('input', {
					type: HIDDEN,
					name: name,
					value: data[name]
				}, null, form);
			}
			
			// submit
			form.submit();
			// clean up
			discardElement(form);
			
		}
		post(data)
		
	}
	CData.ready = function(fn){
		if(jqueryReady){
			fn&&fn()
		}else{
			CData.readyQueue.push(fn)
		}
	}
	_CData.prototype = {
		getData:function(opt){
			_CData.log('_CData.prototype.getData')
			if(!this._validateReady('getData',opt))return this;
			if(this._validateAsyns('getData',opt))return this;
			var basesType = opt.bases.type;
			var viewerType = opt.viewer.type;
			
			this[basesType](this["_get_"+basesType+"_options"](opt));
			this[viewerType](this["_get_"+viewerType+"_options"](opt));
			return this;
		},
		setData:function(opt){
			
			_CData.log('_CData.prototype.setData start')
			if(!this._validateReady('setData',opt))return this;
			if(this._validateAsyns('setData',opt))return this;
			_CData.log('_CData.prototype.setData end')
			var self = this;
			self.DATAS[self.time] = opt;
			return self;
		},
		_updateDATAS:function(){
			
		 	var self = this;
			var _DATAS = $.extend(false,{},self.DATAS[self.time]);

			
			var DATAS = self._formatData(_DATAS);
			var _REALTIMEDATAS = $.extend(false,{},self.REALTIMEDATAS[self.time]);
			var REALTIMEDATAS = self._formatData(_REALTIMEDATAS);
			
			var dseries = DATAS.series;
			var dcategories = DATAS.categories;
			var dlen = dseries.length;
		
			var series = REALTIMEDATAS.series;
			var len = series.length;
			for(var i=0;i<len;i++){
				var serie = series[i];
				var name = serie.name;
				for(var j=0;j<dlen;j++){
					var dserie = dseries[j];
					if(dserie.name==name){
						dserie.data.push(serie.data[0]);
						dserie.data.shift();
					}
				}
			};
			dcategories.push(REALTIMEDATAS.categories[0]);
			dcategories.shift();
		
			self.DATAS[self.time] = _DATAS;
			
		},
		_formatData:CData.formatData,
		dashboard:function(opt){
			var self = this;
			_CData.log('_CData.prototype.dashboard start')
			if(!this._validateReady('dashboard',opt))return this;
			if(this._validateAsyns('dashboard',opt))return this;
			this.asyn = 1;
			var start = opt.start||$.noop;
			var end = opt.end||$.noop;
			var callback = opt.callback||$.noop;
			
			
			start();
			require(["dashboard"],function(Dashboard){
				_CData.log('dashboard loaded')
				var dashboard = new Dashboard;
				self.datas.push(dashboard);
				self.methods['dashboard'] = dashboard;
				opt["__cdata_caller__"] = CData.queue[self.time];
				_CData.log('_CData.prototype.dashboard opt::',opt)
				dashboard.fetch(opt,function(t){
					var isRealtime = t.isRealtime;
					var data = t.data;
					end(data);
					
					_CData.log('_CData.prototype.dashboard end')
					
					// REALTIMEDATAS
					if(isRealtime){
						self.REALTIMEDATAS[self.time] = data;
						self._updateDATAS();
						self._refreshShows();
					}else{
						self.DATAS[self.time] = data;
					}
					self.DATATYPE = "dashboard";
					self.asyn = 0;    
					self._clearCurrentASYNS();
				})
			})
			return this;
		},
		hbase:function(opt){
			
			var self = this;
			_CData.log('_CData.prototype.hbase start')
			if(!this._validateReady('hbase',opt))return this;
			if(this._validateAsyns('hbase',opt))return this;
			this.asyn = 1;
			var start = opt.start||$.noop;
			var end = opt.end||$.noop;
			var callback = opt.callback||$.noop;
			start();
			require(["hbase"],function(Hbase){
				_CData.log('hbase loaded')
				var hbase = new Hbase;
				self.datas.push(hbase);
				self.methods['hbase'] = hbase;
				hbase.fetch(opt,function(t){
					end(t);
					_CData.log('_CData.prototype.hbase end')
					if(opt.filter)t = opt.filter(t);
					self.DATAS[self.time] = t;
					self.DATATYPE = "Hbase";
					self.asyn = 0;    
					self._clearCurrentASYNS();
				})
			})
			return this;
		},
		
		mysql:function(opt){
			var self = this;
			_CData.log('_CData.prototype.summary start')
			if(!this._validateReady('mysql',opt))return this;
			if(this._validateAsyns('mysql',opt))return this;
			this.asyn = 1;
			require(["mysql"],function(mysql){
				_CData.log('mysql loaded')
				mysql.fetch(opt,function(t){
					_CData.log('_CData.prototype.mysql end')
					if(opt.filter)t = opt.filter(t);
					self.DATAS[self.time] = t;
					self.DATATYPE = "mysql";
					self.asyn = 0;    
					self._clearCurrentASYNS();
				})
			})
			return this;
		},
		postgresql:function(opt){
			var self = this;
			_CData.log('_CData.prototype.summary start')
			if(!this._validateReady('postgresql',opt))return this;
			if(this._validateAsyns('postgresql',opt))return this;
			this.asyn = 1;
			require(["postgresql"],function(postgresql){
				_CData.log('postgresql loaded')
				postgresql.fetch(opt,function(t){
					_CData.log('_CData.prototype.postgresql end')
					if(opt.filter)t = opt.filter(t);
					
					self.DATAS[self.time] = t;
					self.DATATYPE = "postgresql";
					self.asyn = 0;    
					self._clearCurrentASYNS();
				})
			})
			return this;
		},
		json:function(){},
		csv:function(){},
		metricsTags:function(opt){
			var self = this;
			_CData.log('_CData.prototype.metricsTags start')
			if(!this._validateReady('metricsTags',opt))return this;
			if(this._validateAsyns('metricsTags',opt))return this;
			this.asyn = 1;
			opt["__cdata_caller__"] = CData.queue[self.time];
			require(["metricsTags"],function(MetricsTags){
				_CData.log('_CData.prototype.metricsTags opt::',opt)
				_CData.log('metricsTags loaded')
				var metricsTags = new MetricsTags;
				self.datas.push(metricsTags);
				self.methods['metricsTags'] = metricsTags;
				metricsTags.fetch(opt,function(t){
					var data = t.data;
					_CData.log('_CData.prototype.metricsTags end')
					if(opt.filter)data = opt.filter(data);
					self.DATATYPE = "metricsTags";
					self.asyn = 0;    
					self._clearCurrentASYNS();
				})
			})
			return this;
		}, 
		ready:function(opt){
			//内置模块加载完后执行
			var self = this;
			_CData.log('_CData.prototype.ready start')
			if(!this._validateReady('ready',opt))return this;
			if(this._validateAsyns('ready',opt))return this;
			opt&opt();
			self._clearCurrentASYNS();
			return self;
		},
		pie:function(opt){
			var self = this;
			_CData.log('_CData.prototype.pie start')
			if(!this._validateReady('pie',opt))return this;
			if(this._validateAsyns('pie',opt))return this;
			this.asyn = 1;
			require(["pie"],function(chart){
				var pie = new chart();
				_CData.log('pie commplete')
				_CData.log('_CData.prototype.pie end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.pie data',data)
				pie.setSource(data);
				
				//container will replace id after
				pie.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		line:function(opt){
			var self = this;
			_CData.log('_CData.prototype.line')
			if(!this._validateReady('line',opt))return this;
			if(this._validateAsyns('line',opt))return this;
			this.asyn = 1;
			require(["line"],function(chart){
				var line = new chart();
				self.shows.push(line);
				_CData.log('line commplete')
				_CData.log('_CData.prototype.line end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.line data',data)
				
				var params = opt.params||{};
				var callback = params.callback||$.noop;
				if(data==false){
					callback("error")
				}else{
					callback("success")
					line.setSource(data);
					line.render(opt.id,opt.params);
				}
				
				_CData.log('line',line)
				self._clearCurrentASYNS();
			});
			return self;
			
		},
		area:function(opt){
			var self = this;
			_CData.log('_CData.prototype.area')
			if(!this._validateReady('area',opt))return this;
			if(this._validateAsyns('area',opt))return this;
			this.asyn = 1;
			require(["area"],function(chart){
				var area = new chart();
				self.shows.push(area);
				_CData.log('area commplete')
				_CData.log('_CData.prototype.area end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.area data',data)
				var params = opt.params||{};
				var callback = params.callback||$.noop;
			
				if(data==false){
					callback("error")
				}else{
					callback("success")
					area.setSource(data);
					area.render(opt.id,opt.params);
				}
				
				self._clearCurrentASYNS();
			});
			return this;
			
		},
		bar:function(opt){
			var self = this;
			_CData.log('_CData.prototype.bar')
			if(!this._validateReady('bar',opt))return this;
			if(this._validateAsyns('bar',opt))return this;
			this.asyn = 1;
			require(["bar"],function(chart){
				var bar = new chart();
				_CData.log('bar commplete')
				_CData.log('_CData.prototype.bar end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.bar data',data)
				bar.setSource(data);
				bar.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
			
		},
		scatter:function(opt){
			var self = this;
			_CData.log('_CData.prototype.scatter start')
			if(!this._validateReady('scatter',opt))return this;
			if(this._validateAsyns('scatter',opt))return this;
			this.asyn = 1;
			require(["scatter"],function(chart){
				var scatter = new chart();
				_CData.log('scatter commplete')
				_CData.log('_CData.prototype.scatter end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.scatter data',data)
				scatter.setSource(data);
				scatter.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;                                                                                      
		},
		polar:function(opt){
			var self = this;                                                                                                                                           
			_CData.log('_CData.prototype.polar')
			if(!this._validateReady('polar',opt))return this;
			if(this._validateAsyns('polar',opt))return this;
			this.asyn = 1;
			require(["polar"],function(chart){
				var polar = new chart();
				_CData.log('polar commplete')
				_CData.log('_CData.prototype.polar end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.polar data',data)
				polar.setSource(data);
				polar.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
			
		},
		heatmapgmaps:function(opt){
			var self = this;
			_CData.log('_CData.prototype.heatmapgmaps')
			if(!this._validateReady('heatmapgmaps',opt))return this;
			if(this._validateAsyns('heatmapgmaps',opt))return this;
			this.asyn = 1;
			require(["heatmapgmaps"],function(chart){
				var heatmapgmaps = new chart();
				_CData.log('heatmapgmaps commplete')
				_CData.log('_CData.prototype.heatmapgmaps end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.heatmapgmaps data',data)
				heatmapgmaps.setSource(data);
				heatmapgmaps.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
			
		},
		heatmap:function(opt){
			var self = this;
			_CData.log('_CData.prototype.heatmap')
			if(!this._validateReady('heatmap',opt))return this;
			if(this._validateAsyns('heatmap',opt))return this;
			this.asyn = 1;
			require(["heatmap"],function(chart){
				var heatmap = new chart();
				_CData.log('heatmap commplete')
				_CData.log('_CData.prototype.heatmap end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.heatmap data',data)
				heatmap.setSource(data);
				heatmap.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		funnel:function(opt){
			var self = this;
			_CData.log('_CData.prototype.funnel start')
			if(!this._validateReady('funnel',opt))return this;
			if(this._validateAsyns('funnel',opt))return this;
			this.asyn = 1;
			require(["funnel"],function(chart){
				var funnel = new chart();
				_CData.log('funnel commplete')
				_CData.log('_CData.prototype.funnel end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.funnel data',data)
				funnel.setSource(data);
				funnel.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		bubble:function(opt){
			var self = this;
			_CData.log('_CData.prototype.bubble start')
			if(!this._validateReady('bubble',opt))return this;
			if(this._validateAsyns('bubble',opt))return this;
			this.asyn = 1;
			require(["bubble"],function(chart){
				var bubble = new chart();
				_CData.log('bubble commplete')
				_CData.log('_CData.prototype.bubble end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.bubble data',data)
				bubble.setSource(data);
				bubble.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;                                                                                      
		},
		column:function(opt){
			var self = this;
			_CData.log('_CData.prototype.column')
			if(!this._validateReady('column',opt))return this;
			if(this._validateAsyns('column',opt))return this;
			this.asyn = 1;
			require(["column"],function(chart){
				var column = new chart();
				_CData.log('column commplete')
				_CData.log('_CData.prototype.column end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.column data',data)
				column.setSource(data);
				column.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		combinations:function(opt){
			var self = this;
			_CData.log('_CData.prototype.combinations')
			if(!this._validateReady('combinations',opt))return this;
			if(this._validateAsyns('combinations',opt))return this;
			this.asyn = 1;
			require(["combinations"],function(chart){
				var combinations = new chart();
				_CData.log('combinations commplete')
				_CData.log('_CData.prototype.combinations end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.combinations data',data)
				combinations.setSource(data);
				combinations.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		forceDirected:function(opt){
			var self = this;
			_CData.log('_CData.prototype.forceDirected')
			if(!this._validateReady('forceDirected',opt))return this;
			if(this._validateAsyns('forceDirected',opt))return this;
			this.asyn = 1;
			require(["forceDirected"],function(chart){
				var forceDirected = new chart();
				_CData.log('forceDirected commplete')
				_CData.log('_CData.prototype.forceDirected end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.forceDirected data',data)
				forceDirected.setSource(data);
				forceDirected.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		spacetree:function(opt){
			var self = this;
			_CData.log('_CData.prototype.spacetree')
			if(!this._validateReady('spacetree',opt))return this;
			if(this._validateAsyns('spacetree',opt))return this;
			this.asyn = 1;
			require(["spacetree"],function(chart){
				var spacetree = new chart();
				_CData.log('spacetree commplete')
				_CData.log('_CData.prototype.spacetree end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.spacetree data',data)
				spacetree.setSource(data);
				spacetree.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		spacetree2:function(opt){
			var self = this;
			_CData.log('_CData.prototype.spacetree2')
			if(!this._validateReady('spacetree2',opt))return this;
			if(this._validateAsyns('spacetree2',opt))return this;
			this.asyn = 1;
			require(["spacetree2"],function(chart){
				var spacetree2 = new chart();
				_CData.log('spacetree2 commplete')
				_CData.log('_CData.prototype.spacetree2 end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.spacetree2 data',data)
				spacetree2.setSource(data);
				spacetree2.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		grid:function(opt){
			var self = this;
			_CData.log('_CData.prototype.grid')
			if(!this._validateReady('grid',opt))return this;
			if(this._validateAsyns('grid',opt))return this;
			this.asyn = 1;
			require(["grid"],function(chart){
				var grid = new chart();
				_CData.log('grid commplete')
				_CData.log('_CData.prototype.grid end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.grid data',data)
				grid.setSource(data);
				grid.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		waterfall:function(opt){
			var self = this;
			_CData.log('_CData.prototype.waterfall')
			if(!this._validateReady('waterfall',opt))return this;
			if(this._validateAsyns('waterfall',opt))return this;
			this.asyn = 1;
			require(["waterfall"],function(chart){
				var waterfall = new chart();
				_CData.log('waterfall commplete')
				_CData.log('_CData.prototype.waterfall end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.waterfall data',data)
				waterfall.setSource(data);
				waterfall.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		vectorMap:function(opt){
			var self = this;
			_CData.log('_CData.prototype.vectorMap')
			if(!this._validateReady('vectorMap',opt))return this;
			if(this._validateAsyns('vectorMap',opt))return this;
			this.asyn = 1;
			require(["vectorMap"],function(chart){
				var vectorMap = new chart();
				_CData.log('vectorMap commplete')
				_CData.log('_CData.prototype.vectorMap end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.vectorMap data',data)
				vectorMap.setSource(data);
				vectorMap.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		vectorMap2:function(opt){
			var self = this;
			_CData.log('_CData.prototype.vectorMap2')
			if(!this._validateReady('vectorMap2',opt))return this;
			if(this._validateAsyns('vectorMap2',opt))return this;
			this.asyn = 1;
			require(["vectorMap2"],function(chart){
				var vectorMap2 = new chart();
				_CData.log('vectorMap2 commplete')
				_CData.log('_CData.prototype.vectorMap2 end')
				self.asyn = 0;
				var data = self.DATAS[self.time];
				_CData.log('_CData.prototype.vectorMap2 data',data)
				vectorMap2.setSource(data);
				vectorMap2.render(opt.id,opt.params);
				self._clearCurrentASYNS();
			});
			return this;
		},
		_refreshShows:function(){
			var self = this;
			var shows = self.shows;
			var len = shows.length;
			for(var  i=0;i<len;i++){
				var show = shows[i];
				if(show.addPoint){
					show.addPoint(self.REALTIMEDATAS[self.time]);
				}else if(show.refresh){
					show.refresh(self.DATAS[self.time]);
				}
			}
			
		},
		
		
		//tools start
		_export_data:function(){},
		_improt_data:function(){},
		_multiple_layer_change:function(){},
		//tools end
		
		//config module start
		config:function(){},
		//config module end
		
		//data cache module start
		data_cache:function(){},
		//data cache module end
		
		//add full screen function
		full_screen:function(){},
		
		
		
		_validateAsyns:function(name,opt){
			var self = this;
			_CData.log('_CData.prototype._validateAsyns',name,this.asyn)
			if(this.asyn){
				self.ASYNS.push({
					name:name,
					opt:opt
				})
				return 1;
			}else{
				return 0;
			}
		},
		_validateReady:function(name,opt){
			_CData.log('_CData.prototype._validateReady',name,jqueryReady)
			if(jqueryReady){
				CData.queue[this.time]['complete'] = 1;
				return 1;
			}else{
				CData.queue[this.time]['fns'].push({
					name:name,
					opt:opt
				})
				
				return 0;
			}
		},
		_clearCurrentASYNS:function(){
			
			var self = this;
			_CData.log('_CData.prototype._clearCurrentASYNS',self.ASYNS)
			var tempASYNS = [];
			var len = self.ASYNS.length;
			for(var i=0;i<len;i++){
				tempASYNS.push(self.ASYNS[i])
			}
			self.ASYNS = [];
			var tempASYNSLen = tempASYNS.length;
			for(var i=0;i<tempASYNSLen;i++){
				var first = tempASYNS.shift();
				self[first.name].call(self,first.opt);
			}
		},
		_get_summary_options:function(opt){
			var dafaults = _CData.defaults['summary'];   
			var tempOpt = this._options_filter(dafaults,opt)
			return tempOpt;
		},
		_get_detail_options:function(opt){
			var dafaults = _CData.defaults['detail'];   
			var tempOpt = this._options_filter(dafaults,opt)
			return tempOpt;
		},
		_get_pie_options:function(opt){
			var dafaults = _CData.defaults['pie'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},
		_get_line_options:function(opt){
			var dafaults = _CData.defaults['line'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},
		_get_area_options:function(opt){
			var dafaults = _CData.defaults['area'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},
		_get_bar_options:function(opt){
			var dafaults = _CData.defaults['bar'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},
		_get_scatter_options:function(opt){
			var dafaults = _CData.defaults['scatter'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},
		_get_polar_options:function(opt){
			var dafaults = _CData.defaults['polar'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},	
		_get_heatmapgmaps_options:function(opt){
			var dafaults = _CData.defaults['heatmapgmaps'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},	
		_get_heatmap_options:function(opt){
			var dafaults = _CData.defaults['heatmap'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},	
		_get_funnel_options:function(opt){
			var dafaults = _CData.defaults['funnel'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},
		_get_bubble_options:function(opt){
			var dafaults = _CData.defaults['bubble'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},
		_options_filter:function(o1,o2){
			var o = {};
			for(var n in o1){
				o[n] = o1[n];
			}
			for(var n in o){
				o[n] = o2[n];
			}
			return o;
		},
		_get_column_options:function(opt){
			var dafaults = _CData.defaults['column'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},
		_get_combinations_options:function(opt){
			var dafaults = _CData.defaults['combinations'];   
			var tempOpt = this._options_filter(dafaults,opt["viewer"])
			return tempOpt;
		},
		destroy:function(){
			var self = this;
			var shows = self.shows;
			var len = shows.length;
			for(var  i=0;i<len;i++){
				var show = shows[i];
				if(show.destroy){
					show.destroy();
				}
				show = null;	
			}
			var datas = self.datas;
			var len = datas.length;
			for(var  i=0;i<len;i++){
				var datas = datas[i];
				if(datas.destroy){
					datas.destroy();
				}
				datas = null;	
			}
		},
		"reload":function(){
			CData.log("CData::reload")
			var self = this;
			var shows = self.shows;
			var len = shows.length;
			for(var  i=0;i<len;i++){
				var show = shows[i];
				if(show.refresh){
					show.refresh(self.DATAS[self.time]);
				}
			}
		}
	}
	_CData.defaults = {
		summary:{
			"pageid":"100101",
			"bases":{
				"type": "summary",
				"startTime":"2013-04-26 10:13:55",
				"endTime":"2013-04-26 11:14:10",
				"interval":"1m",
				"metric":"freeway.application.tracelog"
			},
			"statistics":"sum"
		},
		detail:{
		
		},
		pie:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		line:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		area:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		bar:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		column:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		scatter:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		polar:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		heatmapgmaps:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		heatmap:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		funnel:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		},
		bubble:{
			"id":"chart",
			"params":{
				"title": {
					"text": 'freeway.application.tracelog daily'
				}
			}
		}
	}
})(window);

//init 
;(function (){
//	var baseUrl = "http://"+IP+":"+webservicePort+"/DVF"
	var baseUrl = "http://"+IP+":"+webservicePort+"/dashboard-ui/resource"
	var env = "local";
	var config = {  
		baseUrl: baseUrl,  
		paths: {  
			// "jquery": "third-party/jquery/jquery-1.9.1",
			// "jquery": "third-party/jvectormap/jquery-1.8.2.min",
		
			
			"jquery": "third-party/jqGrid-4.3.2/js/jquery-1.7.2.min",
			"highcharts": "third-party/Highcharts-3.0.1/highcharts.src",
			"highchartsmore": "third-party/Highcharts-3.0.1/highcharts-more",
			"highchartsexporting": "third-party/Highcharts-3.0.1/modules/exporting.src",
			"funneljs": "third-party/Highcharts-3.0.1/modules/funnel.src",
			
			"jit": "third-party/Jit/jit",
			
			"mapsapi":"http://maps.google.com/maps/api/js?sensor=false",
			"heatmapjs":"third-party/heatmap/src/heatmap",
			"heatmapgmapsjs":"third-party/heatmap/src/heatmap-gmaps",
	
			"jvectormap2":"third-party/jvectormap/jquery-jvectormap",
			"jvectormap":"third-party/jvectormap/jquery-jvectormap-1.2.2.min",
			"jvectormapdata":"third-party/jvectormap/jquery-jvectormap-cn-merc-en",
			"latlng":"third-party/jvectormap/latlng",
			
			
			"dateExtensions":"third-party/DateExtensions/DP_DateExtensions",
			
			"jqueryuicustommin":"third-party/jqGrid-4.3.2/js/jquery-ui-1.8.2.custom.min",
			"jquerylayout":"third-party/jqGrid-4.3.2/js/jquery.layout",
			"gridlocaleen":"third-party/jqGrid-4.3.2/js/i18n/grid.locale-en",
			"multiselect":"third-party/jqGrid-4.3.2/js/ui.multiselect",
			"jqGrid":"third-party/jqGrid-4.3.2/js/jquery.jqGrid.min",
			"tablednd":"third-party/jqGrid-4.3.2/js/jquery.tablednd",
			"contextmenu":"third-party/jqGrid-4.3.2/js/jquery.contextmenu",
		


			
			"hbase": "_src/plugins/data/Hbase-1.0",
			"dashboard": "_src/plugins/data/dashbord-1.0",
			"mysql": "_src/plugins/data/mysql-1.0",
			"postgresql": "_src/plugins/data/postgresql-1.0",
			
			"metricsTags": "_src/plugins/data/metricsTags-1.0",
		
			
		
			"hbasechart": "_src/plugins/ui/hbasechart-1.0",
			"chart": "_src/plugins/ui/chart-1.0",
			"pie": "_src/plugins/ui/pie-1.0",
			"line": "_src/plugins/ui/line-1.0",
			"area": "_src/plugins/ui/area-1.0",
			"bar": "_src/plugins/ui/bar-1.0",
			"scatter": "_src/plugins/ui/scatter-1.0",
			"polar": "_src/plugins/ui/polar-1.0",
			"heatmapgmaps": "_src/plugins/ui/heatmapgmaps-1.0",
			"heatmap":"_src/plugins/ui/heatmap-1.0",
			"funnel":"_src/plugins/ui/funnel-1.0",
			"bubble":"_src/plugins/ui/bubble-1.0",
			"column":"_src/plugins/ui/column-1.0",
			"combinations":"_src/plugins/ui/combinations-1.0",
			"forceDirected":"_src/plugins/ui/forceDirected-1.0",
			"spacetree":"_src/plugins/ui/spacetree-1.0",
			"spacetree2":"_src/plugins/ui/spacetree-2.0",
			
			"grid":"_src/plugins/ui/grid-1.0",
			
			"waterfall":"_src/plugins/ui/waterfall-1.0",
			
			"vectorMap":"_src/plugins/ui/vectorMap-1.0",
			"vectorMap2":"_src/plugins/ui/vectorMap2-1.0"
			
		

		},
		shim: {
			highcharts: {
				deps: ['jquery'],
				exports: 'highcharts'
			},
			highchartsmore: {
				deps: ['jquery','highcharts'],
				exports: 'highchartsmore'
			},
			highchartsexporting: {
				deps: ['jquery','highcharts','highchartsmore'],
				exports: 'highchartsexporting'
			},
			dateExtensions:{
				deps: [],
				exports: 'dateExtensions'
			},
			funneljs: {
				deps: ['jquery','highcharts'],
				exports: 'funneljs'
			},
			mapsapi: {
				deps: [],
				exports: 'mapsapi'
			},
			heatmapjs: {
				deps: [],
				exports: 'heatmapjs'
			},
			heatmapgmapsjs: {
				deps: ['mapsapi','heatmapjs'],
				exports: 'heatmapgmapsjs'
			},
			forceDirected : {
				deps: [
					'css!third-party/Jit/Examples/css/base.css',
					'css!third-party/Jit/Examples/css/ForceDirected.css'
				]
			},
			spacetree : {
				deps: [
					'css!third-party/Jit/Examples/css/base.css',
					'css!third-party/Jit/Examples/css/Spacetree.css'
				]
			},
			spacetree2: {
				deps: [
					'css!third-party/Jit/Examples/css/base.css',
					'css!third-party/Jit/Examples/css/Other.css'
				]
			},
			grid:{
				deps: [
					'css!third-party/jqGrid-4.3.2/themes/redmond/jquery-ui-1.8.2.custom',
					'css!third-party/jqGrid-4.3.2/themes/ui.jqgrid.css',
					'css!third-party/jqGrid-4.3.2/themes/ui.multiselect.css',
					'jquery',
					'jqueryuicustommin',
					'jquerylayout',
					'gridlocaleen',
					'multiselect',
					'jqGrid',
					'tablednd',
					'contextmenu'
				]
			},
			
			"jvectormap":"third-party/jvectormap/jquery-jvectormap-1.2.2.min",
			"jvectormapdata":"third-party/jvectormap/jquery-jvectormap-de-merc-en",
			"latlng":"third-party/jvectormap/latlng",
			
			latlng:{
				deps: [],
				exports: 'latlng'
			},
			jvectormap:{
				deps: [
					'jquery',
					'latlng'
				],
				exports: 'jvectormap'
			},
			jvectormap2:{
				deps: [
					'jquery',
					'latlng'
				],
				exports: 'jvectormap2'
			},
			jvectormapdata:{
				deps: [
					'jquery',
					'jvectormap'
				],
				exports: 'jvectormapdata'
			},
		
			vectorMap:{
				deps: [
				
					// 'css!third-party/jvectormap/jquery-jvectormap-1.2.2.css',
					'css!third-party/jvectormap/jquery-jvectormap.css',
					'jvectormapdata'
				],
				exports: 'vectorMap'
			}
		}, 
		waitSeconds: 10,
		map: {
		  '*': {
			'css': 'third-party/require-css-master/css'
		  }
		}		
	}
	
	require.config(config);
	//system plugins start
	var system_plugs = ["jquery","latlng"];
	//system plugins end
	if(window.jQuery){
		define( "jquery", [], function () { return jQuery; } );
		require(system_plugs,function(){
			$(function(){
				jQuery_loaded()
			})
		});
	}else{
		require(system_plugs,function(){
			jQuery(function(){
				jQuery_loaded()
			})
		});
	}
	function jQuery_loaded(){
		jqueryReady = true;
		//正则表达式的集合
		var usedRe={
			//browser
			isOpera:/opera/,
			isIE:/msie (\d+)/,
			isFirefox:/firefox\/(\d+)/,
			isChrome:/chrome/,
			isSafari:/safari/,
			isIOS:/iphone|ipod|ipad/,
			isIPhone:/iphone/,
			isIPod:/ipod/,
			isIPad:/ipad/,
			isIPadUCWeb:/ucweb/,
			//判断是空格
			space:/\s+/g,
			//trim
			trimMulti:/^[\s\xA0]+|[\s\xA0]+$/gm,
			trim:/^[\s\xA0]+|[\s\xA0]+$/g,
			//json
			stringifyJSON:/([\x20\n\r\f\\\/\'\"])/g,
			//errStack
			errStack:[
				/^\s*at [^ ]* \((.*?):(\d+):\d+\)$/m,
				/^\s*at (.*?):(\d+):\d+$/m,
				/^\s*@(.*?):\d+$/m
			],
			//判断整数
			isInt:/^-?([1-9]\d*)?\d$/,
			//判断浮点数
			isFloat:/^-?(([1-9]\d*)?\d(\.\d*)?|\.\d+)$/,
			//判断是日期 (yyyy-mm-dd)的格式
			isDate:/^(\d{4})-(\d{1,2})-(\d{1,2})$/,
			//判断是时间 (yyyy-mm-dd h:m:s.ms)的格式
			isDateTime:/^(\d{4})-(\d{1,2})-(\d{1,2}) (\d{1,2}):(\d{1,2}):(\d{1,2})(\.\d+)?$/,
			//extend
			toReStringA:/([\.\\\/\+\*\?\[\]\{\}\(\)\^\$\|])/g,
			toReStringB:/[\r\t\n]/g,
			toDate:/^(\d{4})-(\d{1,2})-(\d{1,2})( \d{1,2}:\d{1,2}:\d{1,2}(\.\d+)?)?$/,
			toDateTime:/^(\d{4})-(\d{1,2})-(\d{1,2})( (\d{1,2}):(\d{1,2}):(\d{1,2})(\.\d+)?)?$/,
			toFormatString:/([yMdhmsS])\1*/g,
			toCurStringA:/(\d)(\d{3}(,|\.|$))/,
			toCurStringB:/^(-?)\./,
			toIntFix:/,/g,
			cssFix:/-(.)/g,
			//offset
			offsetA:/^(none|hidden)$/i,
			//ajax
			ajax:/xml/i,
			//tmpl regexp
			tmplMapString:/[\r\n\'\"\\]/g,
			tmplCheckVari:/(^|[^\.])\b([a-z_$][\w$]*)/gi,
			tmplParse:/^[\s\S]*?(?=(\$\{|\{\{))/,
			tmplKey_$:/^\$\{([\s\S]+)\}$/,
			tmplKey_cmd:/^\{\{\s*(if|else|\/if|\/?loop|\/?each|\/?enum|tmpl)\b.*\}\}$/,
			tmplKey_if:/^\{\{\s*if\s+(.+?)\s*\}\}$/,
			tmplKey_else:/^\{\{\s*else(\s+(.+?))?\s*\}\}$/,
			tmplKey_loop:/^\{\{\s*loop(\s*\(([^,\)]+)(,([^,\)]+))?\))?\s+(.+?)\s*\}\}$/,
			tmplKey_each:/^\{\{\s*each(\s*\(([^,\)]+)(,([^,\)]+))?(,([^,\)]+))?\))?\s+(.+?)\s*\}\}$/,
			tmplKey_enum:/^\{\{\s*enum(\s*\(([^,\)]+)(,([^,\)]+))?\))?\s+(.+?)\s*\}\}$/,
			tmplKey_tmpl:/^\{\{\s*tmpl\s+(.+?)(,([^,]+?))?\s*\}\}$/,
			//self script
			isSelfScript:/cQuery_110421(\.src)?\.js/i
		};

		//add Cdata module
		
		var msie = false;
		var ua=navigator.userAgent.toLowerCase();
		if(arr=ua.match(usedRe.isIE)){
			msie = true;
		}
		$.extend(Function.prototype,{
			bind:function(_this){
				var fn=this;
				var arg=Array.prototype.slice.call(arguments,1);
				return function(){
					return fn.apply(_this,arg.concat(Array.prototype.slice.call(arguments,0)));
				};
			},
			delay:function(t){
				var f=setTimeout(this.wrap(),t);
				f.clear=function(){
					clearTimeout(f)
				};
				return f;
			},
			wrap:function(){
				if (msie){
					return this;
				}else{
					var fn=this;
					return function(){
						try{
							return fn.apply(this);
						}catch(errA){
							CData.log(errA);
							return null;
						}
					};
				}
			}
		})
		
		/** 
		 * @method toDateTime
		 * 转换为日期时间型
		 * @return {object} 返回(yyyy-mm-dd h:m:s.ms)格式的日期时间对象
		 */ 
		 $.extend(String.prototype,{
			toDateTime:function(){
				var arr=this.match(usedRe.toDateTime);
				if (arr){
					var y=arr[1].toInt(),m=arr[2].toInt()-1,d=arr[3].toInt();
					var h=(arr[5]||'').toInt()||0,min=(arr[6]||'').toInt()||0,s=(arr[7]||'').toInt()||0;
					var t=new Date(y,m,d,h,min,s);
					if (t.getFullYear()==y&&t.getMonth()==m&&t.getDate()==d&&t.getHours()==h&&t.getMinutes()==min&&t.getSeconds()==s)
						return t;
				}
				return null;
			},
			toInt : function(){
				return parseInt(this.replace(usedRe.toIntFix,''),10);
			},
			repeat:function(times){
				var arr=[];
				arr[times]='';
				return arr.join(this);
			},
			isDate:function(){
				var arr=this.match(usedRe.isDate);
				if (arr){
					var y=arr[1].toInt(),m=arr[2].toInt()-1,d=arr[3].toInt();
					var t=new Date(y,m,d);
					if (t.getFullYear()==y&&t.getMonth()==m&&t.getDate()==d)
						return true;
				}
				return false;
			},
			isDateTime:function(){
				var arr=this.match(usedRe.isDateTime);
				if (arr){
					var y=arr[1].toInt(),m=arr[2].toInt()-1,d=arr[3].toInt();
					var h=(arr[4]||'').toInt()||0,min=(arr[5]||'').toInt()||0,s=(arr[6]||'').toInt()||0;
					var t=new Date(y,m,d,h,min,s);
					if (t.getFullYear()==y&&t.getMonth()==m&&t.getDate()==d&&t.getHours()==h&&t.getMinutes()==min&&t.getSeconds()==s)
						return true;
				}
				return false;
			},
			replaceWith:function(d) {
				return this.replace(/\{\$(\w+)\}/g, function(a, c) {
					if (c in d) {
						return d[c];
					} else {
						return a;
					}
				})
			}
		 });

		/**
		 * @class cQuery.Date
		 * Date日期的扩展方法
		 * @namespace cQuery
		 */
		$.extend(Date.prototype,{
			/**
			 * @method addYears
			 * 加上特定的年份
			 * @param {integer} 需要加上的特定年份
			 * @return {object} 返回加好年份的日期对象
			 */
			addYears:function(y){
				var d=new Date(+this);
				d.setYear(d.getFullYear()+y);
				return d;
			},
			
			/**
			 * @method addMonths
			 * 加上特定的月份
			 * @param {integer} 需要加上的特定月份
			 * @return {object} 返回加好月份的日期对象
			 */
			addMonths:function(M){
				var d=new Date(+this);
				d.setMonth(d.getMonth()+M);
				return d;
			},
			
			/**
			 * @method addDays
			 * 加上特定的天数
			 * @param {integer} 需要加上的特定天数
			 * @return {object} 返回加好天数的日期对象
			 */
			addDays:function(D){
				var d=new Date(+this);
				d.setDate(d.getDate()+D);
				return d;
			},
			
			/**
			 * @method addHours
			 * 加上特定的小时数
			 * @param {integer} 需要加上的特定小时数
			 * @return {object} 返回加好小时数的日期对象
			 */
			addHours:function(h){
				var d=new Date(+this);
				d.setHours(d.getHours()+h);
				return d;
			},
			
			/**
			 * @method addMinutes
			 * 加上特定的分钟数
			 * @param {integer} 需要加上的特定分钟数
			 * @return {object} 返回加好分钟数的日期对象
			 */
			addMinutes:function(m){
				var d=new Date(+this);
				d.setMinutes(d.getMinutes()+m);
				return d;
			},
			
			/**
			 * @method addSeconds
			 * 加上特定的秒数
			 * @param {integer} 需要加上的特定秒数
			 * @return {object} 返回加好秒数的日期对象
			 */
			addSeconds:function(s){
				var d=new Date(+this);
				d.setSeconds(d.getSeconds()+s);
				return d;
			},
			/**
			 * @method toDate
			 * 生成对应的日期对象
			 * @return {object} 返回日期对象
			 */
			toDate:function(){
				return new Date(this.getFullYear(),this.getMonth(),this.getDate());
			},
			
			/**
			 * @method toStdDateString
			 * 生成对应的日期字符串
			 * @return {string} 返回日期字符串
			 */
			toStdDateString:function(){
				return this.getFullYear()+'-'+(this.getMonth()+1)+'-'+this.getDate();
			},

			toStdDateTimeString:function(){
				function checkTime(i) { 
					if (i<10) { 
						i="0" + i 
					} 
					return i 
				} 
				return this.getFullYear()+'-'+checkTime(this.getMonth()+1)+'-'+checkTime(this.getDate())+' '+checkTime(this.getHours())+':'+checkTime(this.getMinutes())+':'+checkTime(this.getSeconds());
			},

			/**
			 * @method toEngDateString
			 * 生成对应的英文版日期字符串
			 * @return {string} 返回英文版日期字符串
			 */
			toEngDateString:function(){
				var h=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
				return h[this.getMonth()]+'-'+this.getDate()+'-'+this.getFullYear();
			},
			
			/**
			 * @method toFormatString
			 * 生成需要的格式的日期字符串
			 * @param {string} 格式　例如(yyyy-mm-dd)
			 * @return {string} 返回特定格式的日期字符串
			 */
			toFormatString:function(fmt){
				var h={
					'y':this.getFullYear(),
					'M':this.getMonth()+1,
					'd':this.getDate(),
					'h':this.getHours(),
					'm':this.getMinutes(),
					's':this.getSeconds(),
					'S':this.getMilliseconds()
				};
				var minL={'y':2};
				for (var name in h){
					if (h.hasOwnProperty(name)&&!(name in minL))
						minL[name]=h[name].toString().length;
				}
				return fmt.replace(usedRe.toFormatString,function(a,b){
					var t=h[b];
					var l=Math.max(a.length,minL[b]);
					return ('0'.repeat(l)+t).slice(-l);
				});
			},
			DatePart:function(interval){
				var myDate = this;
				var partStr='';
				var Week = ['日','一','二','三','四','五','六'];
				switch (interval){
					case 'y' :partStr = myDate.getFullYear();break;
					case 'm' :partStr = myDate.getMonth()+1;break;
					case 'd' :partStr = myDate.getDate();break;
					case 'w' :partStr = Week[myDate.getDay()];break;
					case 'ww' :partStr = myDate.WeekNumOfYear();break;
					case 'h' :partStr = myDate.getHours();break;
					case 'n' :partStr = myDate.getMinutes();break;
					case 's' :partStr = myDate.getSeconds();break;
				}
				return parseInt(partStr,10);
			}
		});
		Array.prototype.clone=function(){ return [].concat(this); } 
		
		for(var n=0;n<CData.readyQueue.length;n++){
			var fn = CData.readyQueue[n];
			fn&&fn();
		}
		for(var key in CData.queue){
			var obj = CData.queue[key];
			var ins = obj;
			if(!ins.complete){
				var constructor = ins.constructor;
				$(ins.fns).each(function(index1,obj1){
					constructor[obj1.name].call(constructor,obj1.opt)
				})
				ins.complete = 1;
			}
		}
	}
})();


