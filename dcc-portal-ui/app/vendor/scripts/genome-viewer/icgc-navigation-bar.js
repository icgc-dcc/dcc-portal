/*
 * Copyright (c) 2012 Francisco Salavert (ICM-CIPF)
 * Copyright (c) 2012 Ruben Sanchez (ICM-CIPF)
 * Copyright (c) 2012 Ignacio Medina (ICM-CIPF)
 *
 * This file is part of JS Common Libs.
 *
 * JS Common Libs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * JS Common Libs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JS Common Libs. If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

function IcgcNavigationBar(args) {

  // Using Underscore 'extend' function to extend and add Backbone Events
  _.extend(this, Backbone.Events);

  this.id = Utils.genId('IcgcNavigationBar');

  this.species = 'Homo sapiens';
  this.increment = 3;

  //set instantiation args, must be last
  _.extend(this, args);

  //set new region object
  this.region = new Region(this.region);

  this.currentChromosomeList = [];

  this.on(this.handlers);

  this.els =  {};

  this.rendered = false;
  if (this.autoRender) {
    this.render();
  }
}

IcgcNavigationBar.prototype = {

  render: function(targetId) {
    var _this, target;

    _this = this;
    this.targetId = targetId || this.targetId;
    target = jQuery('.gv-navbar-zoom');


    if (target.length < 1) {
      console.log('targetId not found in DOM');
      return;
    }

    var navgationHtml =
//        '<button id="restoreDefaultRegionButton" class="t_button"><i class="icon-repeat"></i></button> ' +
        '<span class="btn-group" style="font-size: inherit; vertical-align: baseline">' +
        '<button id="regionHistoryButton" class="t_button" data-toggle="dropdown">' +
        '<i class="icon-time"></i> <span class="icon-caret-down"></span></button>' +
        '<ul id="regionHistoryMenu" class="dropdown-menu"></ul>' +
        '</span>' +
        '<span style="margin:0 .25rem">' +
        '<button id="zoomOutButton" class="t_button"><i style="background:none !important;" class="icon-zoom-out"></i></button>' +
        '<span style="margin:0 15px"><span id="slider" style="display:inline-block;width:100px" ></span></span>' +
        '<button id="zoomInButton" class="t_button"><i style="background:none !important;" class="icon-zoom-in"></i></button> ' +
        '</span>';

//        icon-repeat
    this.targetDiv = target[0];
    this.div = jQuery('<span style="" ' +
      'class="unselectable" ' +
      'id="navigation-bar">' + navgationHtml + '</span>')[0];
    // @icgc - need Nav functions but not the bar
    jQuery(this.targetDiv).append(this.div);

    var els = $(this.div).find('*[id]');
    for (var i = 0; i < els.length; i++) {
        var elid = els[i].getAttribute('id');
        if (elid) {
            this.els[elid] = els[i];
        }
    }

    $(this.els.restoreDefaultRegionButton).click(function (e) {
        _this.trigger('restoreDefaultRegion:click', {clickEvent: e, sender: {}})
    });

    this._addRegionHistoryMenuItem(this.region);

    this.zoomSlider = jQuery(this.div).find('#slider');
    jQuery(this.zoomSlider).slider({
      min: 0,
      max: 100,
      value: this.zoom,
      step: 0.000001,
      tooltip: 'hide'
    }).on('slideStop', function(ev) {
        _this._handleZoomSlider(ev.value);
      });

//        {
//            range: "min",
//            value: this.zoom,
//            min: 0,
//            max: 100,
//            step: Number.MIN_VALUE,
//            stop: function (event, ui) {
//                _this._handleZoomSlider(ui.value);
//            }
//        });

//        jQuery('#foo').slider().on();

    this.zoomInButton = jQuery(this.div).find('#zoomInButton');
    this.zoomOutButton = jQuery(this.div).find('#zoomOutButton');
    jQuery(this.zoomOutButton).click(function() {
      _this._handleZoomOutButton();
    });
    jQuery(this.zoomInButton).click(function() {
      _this._handleZoomInButton();
    });

    this.rendered = true;
  },
  draw: function() {
    if (!this.rendered) {
      console.info(this.id + ' is not rendered yet');
    }
  },

    _addRegionHistoryMenuItem: function (region) {
        var _this = this;
        var menuEntry = $('<li role="presentation"><a tabindex="-1" role="menuitem">' + region.toString() + '</a></li>')[0];
        $(this.els.regionHistoryMenu).append(menuEntry);
        $(menuEntry).click(function () {
            var region = new Region($(this).text());
            _this._triggerRegionChange({region: region, sender: _this})
        });
    },

    _handleZoomOutButton: function () {
        this._handleZoomSlider(Math.max(0, this.zoom - 1));
    },
    _handleZoomSlider: function (value) {
        var _this = this;
        if (!this.zoomChanging) {
            this.zoomChanging = true;
            /**/
            this.zoom = value;
            $(this.zoomSlider).slider('setValue', value);

            this.trigger('zoom:change', {zoom: value, sender: this});
            /**/
            setTimeout(function () {
                _this.zoomChanging = false;
            }, 700);
        }
    },
    _handleZoomInButton: function () {
        this._handleZoomSlider(Math.min(100, this.zoom + 1));
    },
    setRegion: function (region, zoom) {
        this.region.load(region);
        if (zoom) {
            this.zoom = zoom;
        }
        this._addRegionHistoryMenuItem(region);
    },
    moveRegion: function (region) {
        this.region.load(region);
    },
    setWidth: function (width) {
        this.width = width;
    },
  setVisible: function(obj) {
      for (key in obj) {
          var query = $(this.els[key]);
          if (obj[key]) {
              query.show();
          } else {
              query.hide();
          }
      }
  },
    _triggerRegionChange: function (event) {
        var _this = this;
        if (!this.regionChanging) {
            this.regionChanging = true;
            /**/
            this.trigger('region:change', event);
            /**/
            setTimeout(function () {
                _this.regionChanging = false;
            }, 700);
        }
    },
  setFullScreenButtonVisible: function(bool) {
    this.fullscreenButton.setVisible(bool);
  }
};

