import { path as treePath } from 'tree';

export function canGoForward(ctrl) {
  return ctrl.vm.currentNode.children.length > 0;
}

export function next(ctrl) {
  let child = ctrl.vm.currentNode.children[0];
  if (!child) return;
  ctrl.userJump(ctrl.vm.currentPath + child.id);
}

export function prev(ctrl) {
  ctrl.userJump(treePath.init(ctrl.vm.currentPath));
}

export function last(ctrl) {
  let toInit = !treePath.contains(ctrl.vm.currentPath, ctrl.vm.initialPath);
  ctrl.userJump(
    toInit ? ctrl.vm.initialPath : treePath.fromNodeList(ctrl.vm.mainline)
  );
}

export function first(ctrl) {
  let toInit = ctrl.vm.currentPath !== ctrl.vm.initialPath && treePath.contains(ctrl.vm.currentPath, ctrl.vm.initialPath);
  ctrl.userJump(
    toInit ? ctrl.vm.initialPath : treePath.root
  );
}
