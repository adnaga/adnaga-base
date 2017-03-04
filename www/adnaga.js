function deepmerge(target, src) {
  var array = Array.isArray(src);
  var dst = array && [] || {};

  if (array) {
    target = target || [];
    dst = dst.concat(target);
    src.forEach(function (e, i) {
      if (typeof dst[i] === 'undefined') {
        dst[i] = e;
      } else if (typeof e === 'object') {
        dst[i] = deepmerge(target[i], e);
      } else {
        if (target.indexOf(e) === -1) {
          dst.push(e);
        }
      }
    });
  } else {
    if (target && typeof target === 'object') {
      Object.keys(target).forEach(function (key) {
        dst[key] = target[key];
      })
    }
    Object.keys(src).forEach(function (key) {
      if (typeof src[key] !== 'object' || !src[key]) {
        dst[key] = src[key];
      }
      else {
        if (!target[key]) {
          dst[key] = src[key];
        } else {
          dst[key] = deepmerge(target[key], src[key]);
        }
      }
    });
  }
  return dst;
}

function log (logLine) {
  console.log('[adnaga] ' + logLine)
}

module.exports = {
  /////// states
  _inited: false,
  _adsOptions: {
    showCooldown: 60, // default showCooldown in seconds for each network
    loadCooldown: 25, // default loadCooldown in seconds for each network
    globalShowCooldown: 0, // 0 means disabled. Otherwise it is a global show cooldown in seconds
    initLastShow: 0,
    maxLoadRetry: -1, // -1 means no limit
    networks: {
      // network: { name: network, pid: null, weight: 100 },

      // admob: { name: 'admob', pid: null, weight: 100 },
      // applovin: { name: 'applovin', pid: null, weight: 100, maxLoadRetry: -1 },
      // adcolony: { name: 'adcolony', pid: null, weight: 100, maxLoadRetry: -1 },
      // cb: { name: 'cb', pid: null, weight: 100 },
    },
  },
  _lastShow: 0,
  _adsStates: {
    // network: { ready: false, lastShow: 0, lastLoad: 0, loadFailCount: 0, autoReload: true/false }

    // admob: { ready: false, lastShow: 0, lastLoad: 0, loadFailCount: 0 },
    // applovin: { ready: false, lastShow: 0, lastLoad: 0, loadFailCount: 0 },
    // adcolony: { ready: false, lastShow: 0, lastLoad: 0, loadFailCount: 0 },
    // cb: { ready: false, lastShow: 0, lastLoad: 0, loadFailCount: 0 },
  },

  configAds: function (options, successCallback, errorCallback) {
    var availableNetworks = 0
    // firstly, try to get the default network settings from js
    for (var network in options.networks) {
      if (options.networks[network].pid === null) {
        delete options.networks[network]
        continue
      }
      // for a valid adnaga-pluin, for example network name = nwk1
      // window['nwk1'] will have defaultConfig and autoReload exported
      if (window['adnaga_' + network] && window['adnaga_' + network].defaultConfig) {
        this._adsOptions.networks[network] = window['adnaga_' + network].defaultConfig
        this._adsStates[network] = { ready: false, lastShow: 0, lastLoad: 0, loadFailCount: 0, autoReload: window['adnaga_' + network].autoReload }
        availableNetworks++
      } else {
        log('[error] network:' + network + ' is not available, did you forget to add the cordova plugin??')
        delete options.networks[network]
      }
    }
    // then, merge the input config with the default one
    this._adsOptions = deepmerge(this._adsOptions, options)
    // now, apply default cooldown to every network config
    for (var property in this._adsOptions.networks) {
      if (!this._adsOptions.networks[property].showCooldown) {
        this._adsOptions.networks[property].showCooldown = this._adsOptions.showCooldown
      }
      if (!this._adsOptions.networks[property].loadCooldown) {
        this._adsOptions.networks[property].loadCooldown = this._adsOptions.loadCooldown
      }
      if (!this._adsOptions.networks[property].maxLoadRetry) {
        this._adsOptions.networks[property].maxLoadRetry = this._adsOptions.maxLoadRetry
      }
    }
    // set the initLastShow before starting, to prevent showing ads too quickly
    for (var network in this._adsStates) {
      this._adsStates[network].lastShow = this._adsOptions.initLastShow
    }
    log('Actual adsOption = ' + JSON.stringify(this._adsOptions, null, 2))
    log(availableNetworks + ' network(s) available in this session')

    // call java init method
    //   initSettings would be network1:pid1|network2:pid2|network3:pid3|...
    var initSettings = ''
    for (var network in this._adsOptions.networks) {
      var networkObj = this._adsOptions.networks[network]
      if (networkObj && networkObj.pid) {
        initSettings += (network + ':' + networkObj.pid + '|')
      }
    }

    var self = this
    cordova.exec(function (adsEvent) {
        if (adsEvent && adsEvent.network_name) {
          // adnaga_event contains network_name, event_name, event_detail
          //   event_name = START | READY | FINISH | LOADERROR | CLICK
          cordova.fireWindowEvent('adnaga_event', adsEvent)
          if (adsEvent.event_name === 'READY') {
            log('[warn] ' + adsEvent.network_name + ' loaded ok, is ready.')
            // set the ready state
            self._adsStates[adsEvent.network_name].ready = true
          } else if (adsEvent.event_name === 'LOADERROR') {
            // load again when load failed
            self._adsStates[adsEvent.network_name].loadFailCount++
            if (self._adsOptions.networks[adsEvent.network_name].maxLoadRetry > 0 &&
                self._adsOptions.networks[adsEvent.network_name].maxLoadRetry < self._adsStates[adsEvent.network_name].loadFailCount) {
              log('[warn] max loaderror count reached, will no longer load for: ' + adsEvent.network_name)
            } else {
              self._loadAds(adsEvent.network_name)
            }
          } else if (adsEvent.event_name === 'FINISH') {
            // after ads is dismissed, load it again.
            // Here also set the lastShow time again, make the show pattern more reasonable
            self._adsStates[adsEvent.network_name].lastShow = new Date().getTime()
            self._lastShow = new Date().getTime()
            self._loadAds(adsEvent.network_name)
          }
        } else { // not adsEvent, means first callback when init is done.
          log('Adnaga.init is done')
          // now load ads from all available networks
          for (var property in self._adsStates) {
            self._loadAds(property)
          }
          // hookup the onPause and onResume
          document.addEventListener('pause',
            function () {
              cordova.exec(function () { }, function () { }, 'Adnaga', 'onPause', [])
            }, false)
          document.addEventListener('resume',
            function () {
              cordova.exec(function () { }, function () { }, 'Adnaga', 'onResume', [])
            }, false)
        }
      }, function (err) {
        log('[error] failed to call Adnaga.init', err)
        cordova.fireWindowEvent("adnaga_init_failure", { type: 'init_failure', error: err })
      }, 'Adnaga', 'init', [ initSettings ])

    successCallback(this._adsOptions)
  },

  showAds: function (delay, successCallback, errorCallback) {
    // get all the available ads, based on java layer and showCooldown
    // then based on weight, pick one of them.
    delay = delay < 0 ? 0 : delay
    var networkToShow = this.pickNextAdsToShow()
    if (networkToShow) {
      this._adsStates[networkToShow].ready = false
      this._adsStates[networkToShow].lastShow = new Date().getTime()
      this._lastShow = new Date().getTime()
      setTimeout(function () {
          cordova.exec(successCallback, errorCallback,
            'Adnaga', 'showAds', [ networkToShow ])
        }, delay)
    } else {
      errorCallback('no ready ads to show')
    }
  },

  getAdsStates: function () {
    return this._adsStates
  },

  // return an array with network_name in it
  getAvailableAds: function () {
    var result = []
    var now = new Date().getTime()
    for (var network in this._adsStates) {
      if (this._adsStates[network].ready) {
        var desiredShowTime = this._adsStates[network].lastShow
        desiredShowTime += this._adsOptions.networks[network].showCooldown * 1000
        if (desiredShowTime <= now) {
          result.push(network)
        }
      }
    }
    return result
  },

  pickNextAdsToShow: function () {
    // check the globalShowCooldown settings first
    if (this._adsOptions.globalShowCooldown > 0 &&
        this._lastShow + this._adsOptions.globalShowCooldown * 1000 > new Date().getTime()) {
      log('[info] globalShowCooldown is set, and it is still in it, return null')
      return null
    }
    var readyOnes = this.getAvailableAds()
    if (readyOnes.length === 0) {
      return null
    }
    log('[info] try to pick next ads to show from ' + readyOnes.length + ' choices')
    // based on weight, do some random calc
    var totalWeight = 0
    var winner = readyOnes[0]
    for (var ix = 0; ix < readyOnes.length; ix++) {
      totalWeight += this._adsOptions.networks[readyOnes[ix]].weight
    }
    var rand = Math.floor(Math.random() * totalWeight)
    for (var iy = 0; iy < readyOnes.length; iy++) {
      var curItemWeight = this._adsOptions.networks[readyOnes[iy]].weight
      if (rand < curItemWeight) {
        winner = readyOnes[iy]
        break
      }
      rand -= curItemWeight
    }
    return winner
  },

  _loadAds: function (networkName) {
    var self = this
    if (self._adsStates[networkName].autoReload) {
      return // no-op, the network will handle the loading itself
    }
    if (self._adsOptions.networks[networkName]) {
      // based on lastLoad ts, calculate the delay of loading
      var now = new Date().getTime()
      var desiredLoadTime = self._adsStates[networkName].lastLoad +
        self._adsOptions.networks[networkName].loadCooldown * 1000
      var delay = now > desiredLoadTime ? 0 : desiredLoadTime - now
      log('[info] will load ' + networkName + ' after ' + delay + ' ms')
      setTimeout(function () {
          self._adsStates[networkName].lastLoad = new Date().getTime()
          cordova.exec(function () { }, function () { },
            'Adnaga', 'loadAds', [ networkName, self._adsOptions.networks[networkName].pid ])
        }, delay)
    } else {
      log('[error] ' + networkName + ' is not enabled in this session, cannot load')
    }
  },
}
