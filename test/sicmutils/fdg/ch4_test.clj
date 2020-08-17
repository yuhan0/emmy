;
; Copyright © 2017 Colin Smith.
; This work is based on the Scmutils system of MIT/GNU Scheme:
; Copyright © 2002 Massachusetts Institute of Technology
;
; This is free software;  you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation; either version 3 of the License, or (at
; your option) any later version.
;
; This software is distributed in the hope that it will be useful, but
; WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
; General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this code; if not, see <http://www.gnu.org/licenses/>.
;

(ns sicmutils.fdg.ch4-test
  (:refer-clojure :exclude [+ - * / zero? ref partial])
  (:require [clojure.test :refer :all]
            [sicmutils.env :refer :all]
            [sicmutils.calculus.manifold :as m]
            [sicmutils.mechanics.rotation :refer [rotate-x-matrix rotate-y-matrix rotate-z-matrix]]
            [sicmutils.simplify :refer [hermetic-simplify-fixture]]))

(use-fixtures :once hermetic-simplify-fixture)

(deftest section-4-0
  (let-coordinates [[x y] R2-rect]
    (let [e0 (+ (* (literal-manifold-function 'e0x R2-rect) d:dx)
                (* (literal-manifold-function 'e0y R2-rect) d:dy))
          e1 (+ (* (literal-manifold-function 'e1x R2-rect) d:dx)
                (* (literal-manifold-function 'e1y R2-rect) d:dy))
          F (literal-manifold-function 'F R2-rect)
          e-vector-basis (down e0 e1)
          e-dual-basis (vector-basis->dual e-vector-basis R2-polar)
          R2-rect-chi-inverse (point R2-rect)
          v (* (up (literal-manifold-function 'b↑0 R2-rect)
                   (literal-manifold-function 'b↑1 R2-rect))
               e-vector-basis)
          p (R2-rect-chi-inverse (up 'X 'Y))]
      ;; e0, 1 are vector fields. They act on manifold functions to
      ;; produce directional derivatives... which are manifold functions.

      (is (= '(+
               (* (e0x (up X Y)) (((partial 0) F) (up X Y)))
               (* (e0y (up X Y)) (((partial 1) F) (up X Y))))
             (simplify ((e0 F) p))))
      (is (= '(+
               (* (((partial 0) F) (up X Y)) (e1x (up X Y)))
               (* (((partial 1) F) (up X Y)) (e1y (up X Y))))
             (simplify ((e1 F) p))))
      (is (= '(down
               (+
                (* (e0x (up X Y)) (((partial 0) F) (up X Y)))
                (* (e0y (up X Y)) (((partial 1) F) (up X Y))))
               (+
                (* (((partial 0) F) (up X Y)) (e1x (up X Y)))
                (* (((partial 1) F) (up X Y)) (e1y (up X Y)))))
             (simplify ((e-vector-basis F) p))))
      (is (= '(up (down 1 0) (down 0 1)) (simplify ((e-dual-basis e-vector-basis) p))))
      (is (= '(up (b↑0 (up X Y)) (b↑1 (up X Y))) (simplify ((e-dual-basis v) p)))))))

(deftest section-4-1
  (let [b-rect ((coordinate-system->oneform-basis R2-rect)
                (literal-vector-field 'b R2-rect))
        b-polar (* (Jacobian (coordinate-system->basis R2-polar)
                             (coordinate-system->basis R2-rect))
                   b-rect)
        p ((point R2-rect) (up 'X 'Y))
        q ((point R2-polar) (up 'R 'Θ))]
    (is (= '(up
             (/
              (+ (* x0 (b↑0 (up x0 y0))) (* y0 (b↑1 (up x0 y0))))
              (sqrt (+ (expt x0 2) (expt y0 2))))
             (/
              (+ (* x0 (b↑1 (up x0 y0))) (* -1 y0 (b↑0 (up x0 y0))))
              (+ (expt x0 2) (expt y0 2))))
           (simplify (b-polar ((point R2-rect) (up 'x0 'y0))))))
    (is (= '(up
             (/
              (+ (* x0 (b↑0 (up x0 y0))) (* y0 (b↑1 (up x0 y0))))
              (sqrt (+ (expt x0 2) (expt y0 2))))
             (/
              (+ (* x0 (b↑1 (up x0 y0))) (* -1 y0 (b↑0 (up x0 y0))))
              (+ (expt x0 2) (expt y0 2))))
           (simplify
            (((coordinate-system->oneform-basis R2-polar)
              (literal-vector-field 'b R2-rect))
             ((point R2-rect) (up 'x0 'y0))))))
    (is (= '(up 0 0) (simplify
                      (-
                       (b-polar ((point R2-rect) (up 'x0 'y0)))
                       (((coordinate-system->oneform-basis R2-polar)
                         (literal-vector-field 'b R2-rect))
                        ((point R2-rect) (up 'x0 'y0)))))))
    (is (= '(up (down (/ X (sqrt (+ (expt X 2) (expt Y 2)))) (* -1 Y))
                (down (/ Y (sqrt (+ (expt X 2) (expt Y 2)))) X))
           (simplify (((coordinate-system->oneform-basis R2-rect)
                       (coordinate-system->vector-basis R2-polar))
                      p))))
    (is (= '(up (down (cos Θ) (* -1 R (sin Θ)))
                (down (sin Θ) (* R (cos Θ))))
           (simplify (((coordinate-system->oneform-basis R2-rect)
                       (coordinate-system->vector-basis R2-polar))
                      q))))
    (is (= '(up (down (/ X (sqrt (+ (expt X 2) (expt Y 2))))
                      (/ Y (sqrt (+ (expt X 2) (expt Y 2)))))
                (down (/ (* -1 Y) (+ (expt X 2) (expt Y 2)))
                      (/ X (+ (expt X 2) (expt Y 2)))))
           (simplify (((coordinate-system->oneform-basis R2-polar)
                       (coordinate-system->vector-basis R2-rect))
                      p))))
    (is (= '(up (down (cos Θ) (sin Θ))
                (down (/ (* -1N (sin Θ)) R) (/ (cos Θ) R)))
           (simplify (((coordinate-system->oneform-basis R2-polar)
                       (coordinate-system->vector-basis R2-rect))
                      q))))
    (let [p (up 'theta 'phi 'psi)
          q (up 'a 'b 'c)
          f #(+ p (* % q))
          M (fn [[theta phi psi]] (* (rotate-z-matrix phi)
                                     (rotate-x-matrix theta)
                                     (rotate-z-matrix psi)))]
      (is (= '(up
               (+ (* a epsilon) theta)
               (+ (* b epsilon) phi)
               (+ (* c epsilon) psi))
             (simplify  (f 'epsilon))))
      (is (= '(up
               (+ (* a epsilon) theta)
               (+ (* b epsilon) phi)
               (+ (* c epsilon) psi))
             (simplify ((comp (chart Euler-angles) (point Euler-angles) f) 'epsilon))))
      (is (= '(up a b c) (simplify ((D f) 'epsilon))))
      (let [g (comp M f)
            h #(* (rotate-x-matrix %) (M (up 'theta 'phi 'psi)))]
        ;; The next two expresssions show the LHS and RHS of the "linear equations" alluded to on
        ;; p.48 of FDG. By equating corresponding entries, we may verify the solution of a, b, c
        ;; given there.
        (is (= '(matrix-by-rows [(+ (* a (sin theta) (sin phi) (sin psi))
                                    (* -1 b (cos phi) (cos theta) (sin psi))
                                    (* -1 c (cos theta) (sin phi) (cos psi))
                                    (* -1 b (sin phi) (cos psi))
                                    (* -1 c (cos phi) (sin psi)))
                                 (+ (* a (sin theta) (sin phi) (cos psi))
                                    (* -1 b (cos phi) (cos theta) (cos psi))
                                    (* c (cos theta) (sin phi) (sin psi))
                                    (* b (sin phi) (sin psi))
                                    (* -1 c (cos phi) (cos psi)))
                                 (+ (* a (cos theta) (sin phi))
                                    (* b (cos phi) (sin theta)))]
                                [(+ (* -1 a (cos phi) (sin theta) (sin psi))
                                    (* -1 b (cos theta) (sin phi) (sin psi))
                                    (* c (cos phi) (cos theta) (cos psi))
                                    (* b (cos phi) (cos psi))
                                    (* -1 c (sin phi) (sin psi)))
                                 (+ (* -1 a (cos phi) (sin theta) (cos psi))
                                    (* -1 b (cos theta) (sin phi) (cos psi))
                                    (* -1 c (cos phi) (cos theta) (sin psi))
                                    (* -1 b (cos phi) (sin psi))
                                    (* -1 c (sin phi) (cos psi)))
                                 (+ (* -1 a (cos phi) (cos theta))
                                    (* b (sin theta) (sin phi)))]
                                [(+ (* a (cos theta) (sin psi))
                                    (* c (sin theta) (cos psi)))
                                 (+ (* a (cos theta) (cos psi))
                                    (* -1 c (sin theta) (sin psi)))
                                 (* -1 a (sin theta))])
               (simplify ((D g) 0))))
        (is (= '(matrix-by-rows [0 0 0]
                                [(* -1 (sin theta) (sin psi))
                                 (* -1 (sin theta) (cos psi))
                                 (* -1 (cos theta))]
                                [(+ (* (cos phi) (cos theta) (sin psi)) (* (sin phi) (cos psi)))
                                 (+ (* (cos phi) (cos theta) (cos psi)) (* -1 (sin phi) (sin psi)))
                                 (* -1 (cos phi) (sin theta))])
               (simplify ((D h) 0))))))))

(deftest section-4-3
  (let-coordinates [[x y] R2-rect]
    (let [e0 (+ (* (literal-manifold-function 'e0x R2-rect) d:dx)
                (* (literal-manifold-function 'e0y R2-rect) d:dy))
          e1 (+ (* (literal-manifold-function 'e1x R2-rect) d:dx)
                (* (literal-manifold-function 'e1y R2-rect) d:dy))
          polar-basis (coordinate-system->basis R2-polar)
          polar-vector-basis (basis->vector-basis polar-basis)
          polar-dual-basis (basis->oneform-basis polar-basis)
          f (literal-manifold-function 'f-rect R2-rect)
          p ((point R2-rect) (up 'X 'Y))]
      (is (zero? (simplify ((- ((commutator e0 e1) f)
                               (* (- (e0 (polar-dual-basis e1))
                                     (e1 (polar-dual-basis e0)))
                                  (polar-vector-basis f)))
                            p))))))
  (let-coordinates [[x y z] R3-rect]
    (let [p ((point R3-rect) (up 'x0 'y0 'z0))
          g (literal-manifold-function 'g-rect R3-rect)
          Jz (- (* x d:dy) (* y d:dx))
          Jx (- (* y d:dz) (* z d:dy))
          Jy (- (* z d:dx) (* x d:dz))]
      (is (zero? (simplify (((+ (commutator Jx Jy) Jz) g) p))))
      (is (zero? (simplify (((+ (commutator Jy Jz) Jx) g) p))))
      (is (zero? (simplify (((+ (commutator Jz Jx) Jy) g) p))))
      (let-coordinates [[theta phi psi] Euler-angles]
        (let [e_x (+ (* (cos phi) d:dtheta)
                     (* -1 (/ (* (sin phi) (cos theta)) (sin theta)) d:dphi)
                     (* (/ (sin phi) (sin theta)) d:dpsi))
              e_y (+ (/ (* (cos phi) (cos theta) d:dphi) (sin theta))
                     (* (sin phi) d:dtheta)
                     (* -1 (/ (cos phi) (sin theta)) d:dpsi))
              e_z d:dphi
              f (literal-manifold-function 'f-Euler Euler-angles)
              q ((point Euler-angles) (up 'theta 'phi 'psi))]
          (is (zero? (simplify (((+ (commutator e_x e_y) e_z) f) q))))
          (is (zero? (simplify (((+ (commutator e_y e_z) e_x) f) q))))
          (is (zero? (simplify (((+ (commutator e_z e_x) e_y) f) q)))))))))
