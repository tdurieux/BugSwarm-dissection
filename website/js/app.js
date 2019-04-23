angular.module('defects4j-website', ['ngRoute', 'ui.bootstrap', 'anguFixedHeaderTable'])
	.config(function($routeProvider, $locationProvider) {
		$routeProvider
			.when('/bug/:benchmark/:id', {
				controller: 'bugController'
			})
			.when('/', {
				controller: 'mainController'
			});
		// configure html5 to get links working on jsfiddle
		$locationProvider.html5Mode(false);
	})
	.directive('keypressEvents', [
		'$document',
		'$rootScope',
		function($document, $rootScope) {
			return {
				restrict: 'A',
				link: function() {
					$document.bind('keydown', function(e) {
						$rootScope.$broadcast('keypress', e);
						$rootScope.$broadcast('keypress:' + e.which, e);
					});
				}
			};
		}
	]).directive('diff', ['$http', function ($http) {
		return {
			restrict: 'A',
			scope: {
				patch: '=diff'
			},
			link: function (scope, elem, attrs) {
				function prepareDiff(diff) {
					if (diff != null && diff != '') {
						var regex_origin = /--- ([^ ]+).*/.exec(diff)
						if (regex_origin) { 
							origin = regex_origin[1]
							dest = /\+\+\+ ([^ ]+).*/.exec(diff)[1]
							if (dest.indexOf(origin) > 0) {
								diff = diff.replace(dest, origin)
							}
							diff = diff.replace(/\\"/g, '"').replace(/\\n/g, "\n").replace(/\\t/g, "\t")
						}
						var diff2htmlUi = new Diff2HtmlUI({ diff: diff });
						diff2htmlUi.draw($(elem), {showFiles: false, matching: 'none'});
						diff2htmlUi.highlightCode($(elem));
					}
				}
				function printDiff(patch) {
					$(elem).text('')
					if (patch.metrics.patchSize > 10000) {
						return $(elem).text('The diff is too big to be displayed')
					}
					var diff = patch.diff;
					if (diff == null) {
						diff = patch.patch;
					}
					if (diff == null) {
						diff = patch.PATCH_DIFF_ORIG;
					}
					if (diff == null) {
						$http.get(patch.benchmark + "/" + patch.bugId + "/" + "patch.diff").then(response => {
							patch.diff = response.data
							prepareDiff(response.data)
						})
					} else {
						prepareDiff(diff)
					}
				}
				scope.$watch('patch', function() {
					printDiff(scope.patch);
				})
				printDiff(scope.patch);
			}
		}
		}])
	.controller('welcomeController', function($uibModalInstance) {
		this.ok = function () {
			$uibModalInstance.close();
		};
	})
	.controller('bugModal', function($scope, $http, $rootScope, $uibModalInstance, bug) {
		var $ctrl = this;
		$ctrl.bug = bug;
		$ctrl.size = 0

		$http.get($ctrl.bug.benchmark + "/" + $ctrl.bug.bugId + "/" + "docker_manifest.json").then(response => {
			for (var layer of response.data.layers) {
				$ctrl.size += layer['size']
			}
			$ctrl.size /= 100000000.0
			$ctrl.size = Math.round($ctrl.size)/10.0
		})

		$rootScope.$on('new_bug', function(e, bug) {
			$ctrl.bug = bug;
			$ctrl.size = 0

			for (var t of document.querySelectorAll('.log')) {
				t.innerText = 'Click here to download the log...'
			}

			$http.get($ctrl.bug.benchmark + "/" + $ctrl.bug.bugId + "/" + "docker_manifest.json").then(response => {

				for (var layer of response.data.layers) {
					$ctrl.size += layer['size']
				}
				$ctrl.size /= 100000000.0
				$ctrl.size = Math.round($ctrl.size)/10.0
			})
		});
		function cleanLog(log) {
			const pattern = [
				'[\\u001B\\u009B][[\\]()#;?]*(?:(?:(?:[a-zA-Z\\d]*(?:;[-a-zA-Z\\d\\/#&.:=?%@~_]*)*)?\\u0007)',
				'(?:(?:\\d{1,4}(?:;\\d{0,4})*)?[\\dA-PR-TZcf-ntqry=><~]))'
			].join('|');
			return log.trim().replace(new RegExp(pattern, 'g'), '')
		}
		$ctrl.show_failing_log = function(e, bug) {
			if (e.target.innerText == 'Loading...') {
				return
			}
			e.target.innerText = 'Loading...'
			$http.get($ctrl.bug.benchmark + "/" + $ctrl.bug.bugId + "/" + "failing.log").then(response => {
				e.target.innerText = ''
				var term = new Terminal({
					cols: 120,
					theme: {
						background: '#FFFFFF',
						foreground: '#333'
					}
				});
				term.open(e.target);
				term.write(response.data)
			})
		}
		$ctrl.show_passing_log = function(e, bug) {
			if (e.target.innerText == 'Loading...') {
				return
			}
			e.target.innerText = 'Loading...'
			$http.get($ctrl.bug.benchmark + "/" + $ctrl.bug.bugId + "/" + "passing.log").then(response => {
				e.target.innerText = ''
				var term = new Terminal({
					cols: 120,
					theme: {
						background: '#FFFFFF',
						foreground: '#333'
					}
				});
				term.open(e.target);
				term.write(response.data)
			})
		}
		$ctrl.ok = function () {
			$uibModalInstance.close();
		};
		$ctrl.nextPatch = function () {
			$rootScope.$emit('next_bug', 'next');
		};
		$ctrl.previousPatch = function () {
			$rootScope.$emit('previous_bug', 'next');
		};
	})
	.controller('bugController', function($scope, $http, $location, $rootScope, $routeParams, $uibModal) {
		var $ctrl = $scope;
		$ctrl.bugs = $scope.$parent.filteredBugs;
		$ctrl.index = -1;
		$ctrl.bug = null;

		$scope.$watch("$parent.filteredBugs", function () {
			$ctrl.bugs = $scope.$parent.filteredBugs;
			$ctrl.index = getIndex($routeParams.benchmark, $routeParams.id);
		});

		var getIndex = function (benchmark, bugId) {
			if ($ctrl.bugs == null) {
				return -1;
			}
			for (var i = 0; i < $ctrl.bugs.length; i++) {
				if ($ctrl.bugs[i].benchmark == benchmark 
					&& ($ctrl.bugs[i].bugId == bugId || bugId == null)) {
					return i;
				}
			}
			return -1;
		};

		$scope.$on('$routeChangeStart', function(next, current) {
			$ctrl.index = getIndex(current.params.benchmark, current.params.id);
		});

		var modalInstance = null;
		$scope.$watch("index", function () {
			if ($scope.index != -1) {
				if (modalInstance == null) {
					modalInstance = $uibModal.open({
						animation: true,
						ariaLabelledBy: 'modal-title',
						ariaDescribedBy: 'modal-body',
						templateUrl: 'modelPatch.html',
						controller: 'bugModal',
						controllerAs: '$ctrl',
						size: "lg",
						resolve: {
							bug: function () {
								return $scope.bugs[$scope.index];
							}
						}
					});
					modalInstance.result.then(function () {
						modalInstance = null;
						$location.path("/");
					}, function () {
						modalInstance = null;
						$location.path("/");
					})
				} else {
					$rootScope.$emit('new_bug', $scope.bugs[$scope.index]);
				}
			}
		});
		var nextPatch = function () {
			var index  = $scope.index + 1;
			if (index == $ctrl.bugs.length)  {
				index = 0;
			}
			$location.path( "/bug/" + $ctrl.bugs[index].benchmark + "/" + $ctrl.bugs[index].bugId );
			return false;
		};
		var previousPatch = function () {
			var index  = $scope.index - 1;
			if (index < 0) {
				index = $ctrl.bugs.length - 1;
			}
			$location.path( "/bug/" + $ctrl.bugs[index].benchmark + "/" + $ctrl.bugs[index].bugId );
			return false;
		};

		$scope.$on('keypress:39', function () {
			$scope.$apply(function () {
				nextPatch();
			});
		});
		$scope.$on('keypress:37', function () {
			$scope.$apply(function () {
				previousPatch();
			});
		});
		$rootScope.$on('next_bug', nextPatch);
		$rootScope.$on('previous_bug', previousPatch);
	})
	.controller('mainController', function($scope, $location, $rootScope, $http, $uibModal) {
		$scope.sortType     = ['benchmark', 'bugId']; // set the default sort type
		$scope.sortReverse  = false;
		$scope.match  = "all";
		$scope.search  = "";
		$scope.filters = {
			'python': true,
			'java': true,
			'changed_test': true
		};
		$scope.benchmarks = ["BugSwarm"];
		$scope.tools = [];
		
		// create the list of sushi rolls 
		$scope.bugs = [];

		function downloadPatches() {
			for (var bench of $scope.benchmarks) {
				$http.get(""+bench.toLowerCase() + ".json").then(function (response) {
					var bugs = response.data
		
					for (var key in bugs){
						$scope.bugs.push(bugs[key]);
					}
		
					var element = angular.element(document.querySelector('#menu')); 
					var height = element[0].offsetHeight;
		
					angular.element(document.querySelector('#mainTable')).css('height', (height-120)+'px');
				});
			}
		}
		downloadPatches();

		

		$scope.openBug = function (bug) {
			$location.path( "/bug/" + bug.benchmark + "/" + bug.bugId );
		};

		$scope.sort = function (sort) {
			if (sort == $scope.sortType || (sort[0] == 'benchmark' && $scope.sortType[0] == 'benchmark')) {
				$scope.sortReverse = !$scope.sortReverse; 
			} else {
				$scope.sortType = sort;
				$scope.sortReverse = false; 
			}
			return false;
		}

		$scope.countBugs = function (key, filter) {
			if (filter == null) {
				filter = {
				}
			}
			if (filter.count) {
				return filter.count;
			}
			var count = 0;
			for(var i = 0; i < $scope.bugs.length; i++) {
				if ($scope.bugs[i].benchmark.toLowerCase() === key.toLowerCase()) {
					count++;
				} else if ($scope.bugs[i].benchmark === key) {
					count++;
				} else if ($scope.bugs[i].repairActions && $scope.bugs[i].repairActions[key] != null && $scope.bugs[i].repairActions[key] > 0) {
					count++;
				} else if ($scope.bugs[i].repairPatterns && $scope.bugs[i].repairPatterns[key] != null && $scope.bugs[i].repairPatterns[key] > 0) {
					count++;
				}
			}
			filter.count = count;
			return count;
		};

		$scope.naturalCompare = function(a, b) {
			if (a.type === "number") {
				return a.value - b.value;
			}
			return naturalSort(a, b);
		}
		$scope.bugsFilter = function (bug, index, array) {
			if ($scope.search) {
				var matchSearch = 
				//bug.failed_job.message.indexOf($scope.search) != -1 ||
				bug.passed_job.message.indexOf($scope.search) != -1 ||
				bug.repo.indexOf($scope.search) != -1
				if (matchSearch === false) {
					return false
				}
			}
			if ($scope.filters['empty'] === true) {
				if (bug.metrics.nbFiles == 0) {
					return false
				}
			}
			if ($scope.filters['commit'] === true) {
				if (bug.unique === false) {
					return false
				}
			}
			if ($scope.filters['unique_diff'] === true) {
				if (bug.unique_diff === false) {
					return false
				}
			}
			if ($scope.filters['test'] === true) {
				if (bug.failed_job.failed_tests == "") {
					return false
				}
			}
			if ($scope.filters['changed_test'] === false) {
				if (bug.changed_test === true) {
					return false
				}
			}
			if ($scope.filters['source'] === true) {
				if (bug.has_source === false) {
					return false
				}
			}
			if ($scope.filters['only_source'] === true) {
				if (bug.only_source === false) {
					return false
				}
			}
			if ($scope.filters['apr'] === true) {
				if (bug.only_source === false || bug.failed_job.failed_tests == "" || bug.unique_diff === false || bug.unique === false || bug.metrics.nbFiles == 0 || bug.changed_test === true || bug.files.added.length > 0 || bug.files.deleted.length > 0) {
					return false
				}
			}
			if ($scope.filters['python'] === false) {
				if (bug.lang === 'Python') {
					return false
				}
			}
			if ($scope.filters['java'] === false) {
				if (bug.lang === 'Java') {
					return false
				}
			}
			return true
		};
	});
