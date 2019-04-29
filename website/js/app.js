angular.module('defects4j-website', ['ngRoute', 'ui.bootstrap', 'anguFixedHeaderTable', 'chart.js'])
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
	.controller('statController', function($uibModalInstance, bugs) {
		var $ctrl = this;
		$ctrl.bugs = bugs;

		$ctrl.options = {
			title: {
				display: true,
				text: 'The time between the failing commit and the passing commit.'
			},
			tooltips: {
				mode: 'index',
				intersect: false
			},
			responsive: true,
			scales: {
				xAxes: [{
					stacked: true,
				}],
				yAxes: [{
					stacked: true
				}]
			}
		}
		$ctrl.fixTimeLabels = ['<1h','<6h','<12h','<24h','<36h','<48h','<1w','<1m','>1m'];
		$ctrl.series = ['Java', 'Python'];


		$ctrl.fixTimeStat = {
			"Python": {3600: 0,21600: 0,43200: 0,86400: 0,129600: 0,172800: 0,604800: 0,2592000: 0,99999999999999: 0},
			"Java": {3600: 0,21600: 0,43200: 0,86400: 0,129600: 0,172800: 0,604800: 0,2592000: 0,99999999999999: 0}
		}
		
		$ctrl.fileStat = {
			"Python": {'modified': 0, "added": 2, "deleted": 0},
			"Java": {'modified': 0, "added": 0, "deleted": 0}
		}

		$ctrl.patchSizeStat = {
			"Python": {"added": 0, "deleted": 0},
			"Java": {"added": 0, "deleted": 0}
		}

		for(var bug of $ctrl.bugs) {
			$ctrl.fileStat[bug.lang].added += bug.files.added.length
			$ctrl.fileStat[bug.lang].modified += bug.files.modified.length
			$ctrl.fileStat[bug.lang].deleted += bug.files.deleted.length

			$ctrl.patchSizeStat[bug.lang].added += bug.metrics.addedLines
			$ctrl.patchSizeStat[bug.lang].deleted += bug.metrics.removedLines

			var failingCommitDate = Date.parse(bug.failed_job.committed_at)
			var passedCommitDate =  Date.parse(bug.passed_job.committed_at)
			if (!failingCommitDate || !passedCommitDate) {
				continue
			}
			var diff = (passedCommitDate - failingCommitDate)/1000
			for (var t in $ctrl.fixTimeStat[bug.lang]) {
				if (diff < t) {
					$ctrl.fixTimeStat[bug.lang][t]++
					break
				}
			}
		}
		$ctrl.fixTimeDate = (function () {
			return [
				Object.values($ctrl.fixTimeStat.Java),
				Object.values($ctrl.fixTimeStat.Python),
			]
		})()

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
				$ctrl.size /= 10737418.24
				$ctrl.size = Math.round($ctrl.size)/100.0
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
		// u
		$scope.$on('keypress:85', function () {
			$ctrl.changeCategory('unknown', $ctrl.nextPatch)
		});
		// b
		$scope.$on('keypress:66', function () {
			$ctrl.changeCategory('bug', $ctrl.nextPatch)
		});
		// n
		$scope.$on('keypress:78', function () {
			$ctrl.changeCategory('nobug', $ctrl.nextPatch)
		});
		$ctrl.changeCategory = function (category, callback) {
			$http.post("/api/categories", {
				bug_id: $ctrl.bug.bugId,
				category: category
			}).then(response => {
				$ctrl.bug.category = category
				if (callback) {
					callback()
				}
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
		$scope.sortType     = ['bugId']; // set the default sort type
		$scope.sortReverse  = false;
		$scope.match  = "all";
		$scope.search  = "";
		$scope.filters = {
			'python': true,
			'java': true,
			'changed_test': true,
			'bug': true,
			'nobug': true,
			'unknown': true,
			'test_failure': true,
			'clone': true,
			'regression': true,
			'main': true,
			'Checkstyle': true,
			'Compilation': true,
			'License': true,
			'Documentation': true,
			'library': true,
			'Unknown': true,
		};
		$scope.benchmarks = ["BugSwarm"];
		$scope.tools = [];
		
		// create the list of sushi rolls 
		$scope.bugs = [];

		function downloadPatches() {
			for (var bench of $scope.benchmarks) {
				$http.get(""+bench.toLowerCase() + ".json").then(function (response) {
					var bugs = response.data
					$http.get("category.json").then(function (response) {
						var categories = response.data

						for (var key in bugs){
							var b = bugs[key]
							if (categories[b.bugId]) {
								b.patch_category = categories[b.bugId].patch_category
								b.failure_category = categories[b.bugId].failure_category
							}
							$scope.bugs.push(b);
						}
					});
		
					var element = angular.element(document.querySelector('#menu')); 
					var height = element[0].offsetHeight;
		
					angular.element(document.querySelector('#mainTable')).css('height', (height-120)+'px');
				});
			}
		}
		downloadPatches();

		
		var statModal = null;
		$scope.openStat = function () {
			statModal = $uibModal.open({
				animation: true,
				ariaLabelledBy: 'modal-title',
				ariaDescribedBy: 'modal-body',
				templateUrl: 'stat.html',
				controller: 'statController',
				controllerAs: '$ctrl',
				size: "lg",
				resolve: {
					bugs: function () {
						return $scope.filteredBugs;
					}
				}
			});
			statModal.result.then(function () {
				statModal = null;
			}, function () {
				statModal = null;
			})
		};
		

		$scope.openBug = function (bug) {
			$location.path( "/bug/" + bug.benchmark + "/" + bug.bugId );
		};

		$scope.sort = function (sort) {
			if (sort == $scope.sortType) {
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
			return naturalSort(a.value, b.value);
		}
		$scope.bugsForAPR = function () {
			$scope.filters.empty = true
			$scope.filters.unique_diff = true
			$scope.filters.test = true
			$scope.filters.commit = true
			$scope.filters.changed_test = false
			$scope.filters.only_source = true
			$scope.filters.image = true
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
				if (bug.failed_job.failed_tests == "" || bug.files.added.length > 0 || bug.files.deleted.length > 0) {
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
			if ($scope.filters.image === true) {
				if (bug.available === false) {
					return false
				}
			}
			if ($scope.filters['bug'] === false) {
				if (bug.patch_category ===  'bug') {
					return false
				}
			}
			if ($scope.filters['nobug'] === false) {
				if (bug.patch_category ===  'nobug') {
					return false
				}
			}
			if ($scope.filters['unknown'] === false) {
				if (bug.patch_category ===  'unknown' || bug.patch_category == null) {
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

			if ($scope.filters.test_failure === false && bug.failure_category ===  'Test') {
				return false
			}
			if ($scope.filters.clone === false && bug.failure_category ===  'Unable to clone') {
				return false
			}
			if ($scope.filters.regression === false && bug.failure_category ===  'Compare  version') {
				return false
			}
			if ($scope.filters.main === false && bug.failure_category ===  'Execution') {
				return false
			}
			if ($scope.filters.Checkstyle === false && bug.failure_category ===  'Checkstyle') {
				return false
			}
			if ($scope.filters.Compilation === false && bug.failure_category ===  'Compilation') {
				return false
			}
			if ($scope.filters.License === false && bug.failure_category ===  'License') {
				return false
			}
			if ($scope.filters.Documentation === false && bug.failure_category ===  'Documentation') {
				return false
			}
			if ($scope.filters.library === false && bug.failure_category ===  'Missing library') {
				return false
			}
			if ($scope.filters.Unknown === false && bug.failure_category ===  'Unknown') {
				return false
			}
			return true
		};
	});
