extendedTapsetInteractives = function () {

  /**
  For all elements of given class,
  if they are  hidden (display == none) the display is set to block
  otherwise it is set to none
  **/
  function showHideBlock(clazz) {
    showHide(clazz, 'block')
  }

  /**
  For all elements of given class,
  if they are  hidden (display == none) the display is set to table-row
  otherwise it is set to none
  **/
  function showHideRow(clazz) {
    showHide(clazz, 'table-row')
  }

  /**
  Shortcut method to show/hide prefixed input classes as table-row
  **/
  function showHideRowGroup(clazz) {
    showHideRow("test_" + clazz)
    showHideRow("tr_details_" + clazz)
  }

  /**
  For all elements of given class,
  if they are  hidden (display == none) the display is set to inline
  otherwise it is set to none
  **/
  function showHideInline(clazz, type) {
    showHide(clazz, 'inline')
  }

  /**
  For all elements of given class,
  if they are  hidden (display == none) the display is set to given type
  otherwise it is set to none
  **/
  function showHide(clazz, type) {
    var all = document.getElementsByClassName(clazz);
    for (var i = 0; i < all.length; i++) {
      if (all[i].style.display == 'none') { all[i].style.display = type } else { all[i].style.display = 'none' };
    }
  }

  /**
  For one exact element of given id
  if it is  hidden (display == none) the display is set to table-row
  otherwise it is set to none
  **/
  function showHideRowOne(id) {
    showHideOne(id, 'table-row')
  }

  /**
  For one exact element of given id
  if it is  hidden (display == none) the display is set to inline
  otherwise it is set to none
  **/
  function showHideInlineOne(id) {
    showHideOne(id, 'inline')
  }

  /**
  For one exact element of given id
  if it is  hidden (display == none) the display is set to block
  otherwise it is set to none
  **/
  function showHideBlockOne(id) {
    showHideOne(id, 'block')
  }

  /**
  For one exact element of given id
  if it is  hidden (display == none) the display is set to given type
  otherwise it is set to none
  **/
  function showHideOne(id, type) {
    var el = document.getElementById(id);
    if (el.style.display == 'none') { el.style.display = type } else { el.style.display = 'none' };
  }

  /**
  Utility method which set display property of all elements of given class to value of type
  **/
  function set(clazz, type) {
    var all = document.getElementsByClassName(clazz);
    for (var i = 0; i < all.length; i++) {
      all[i].style.display = type;
    }
  }

  /**
  Shortcut method to set display of all not-ok elements to table-row, and none to others
  **/
  function showFailed() {
    set("test_ok", "none")
    set("test_not ok", "table-row")
    set("test_ok_SKIP", "none")
    set("tr_details_ok", "none")
    set("tr_details_not ok", "none")
    set("tr_details_ok_SKIP", "none")
    set("detail_body", "none")
  }

  /**
  Shortcut method to set display of all not-ok elements and theirs details to table-row, and none to others
  **/
  function showFailedDetails() {
    set("test_ok", "none")
    set("test_not ok", "table-row")
    set("test_ok_SKIP", "none")
    set("tr_details_ok", "none")
    set("tr_details_not ok", "table-row")
    set("tr_details_ok_SKIP", "none")
    set("detail_body", "block")
  }

  /**
  Shortcut method to set display of all not-ok elements and theirs details to table-row, and none to others.
  ok-skip is set to table-none to hide details body, but keep the its row opened for user-expansion-ondemand
  **/
  function hideDetails() {
    set("test_ok", "none")
    set("test_not ok", "table-row")
    set("test_ok_SKIP", "none")
    set("tr_details_ok", "none")
    set("tr_details_not ok", "table-row")
    set("tr_details_ok_SKIP", "table-none")
    set("detail_body", "none")
  }

  /**
  Shortcut method to set display of all elements to none
  **/
  function showNothing() {
    set("test_ok", "none")
    set("test_not ok", "none")
    set("test_ok_SKIP", "none")
    set("tr_details_ok", "none")
    set("tr_details_not ok", "none")
    set("tr_details_ok_SKIP", "none")
    set("detail_body", "none")
  }

  /**
  Shortcut method to set display of all elements to block or table-row
  **/
  function showAll() {
    set("test_ok", "table-row")
    set("test_not ok", "table-row")
    set("test_ok_SKIP", "table-row")
    set("tr_details_ok", "table-row")
    set("tr_details_not ok", "table-row")
    set("tr_details_ok_SKIP", "table-row")
    set("detail_body", "block")
  }

  return {
    showHideBlock: showHideBlock,
    showHideRow: showHideRow,
    showHideRowGroup: showHideRowGroup,
    showHideInline: showHideInline,
    //showHide: showHide,  //no need to publish
    showHideRowOne: showHideRowOne,
    showHideInlineOne: showHideInlineOne,
    showHideBlockOne: showHideBlockOne,
    showHideOne: showHideOne,
    //set: set,           //no need to publish
    showFailed: showFailed,
    showFailedDetails: showFailedDetails,
    hideDetails: hideDetails,
    showNothing: showNothing,
    showAll: showAll
  }

}
