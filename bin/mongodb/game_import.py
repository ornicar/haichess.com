#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import requests
import logging
import pymongo
import sys
sys.setrecursionlimit(1000000)

logging.basicConfig(filename = "game_import.log", filemode="a", format="%(asctime)s %(name)s:%(levelname)s:%(message)s", datefmt="%d-%M-%Y %H:%M:%S", level=logging.DEBUG)

parser = argparse.ArgumentParser(description = __doc__)
parser.add_argument("--puzzleIDForm", default = 10000, help = "Where to start, default:10000")
parser.add_argument("--mode", default = "dev", choices=["dev", "prod"], help = "dev or prod")
settings = parser.parse_args()
mode = settings.mode
puzzleIDForm = settings.puzzleIDForm

logging.info("Start Import, mode: %s, puzzleIDForm: %s", mode, puzzleIDForm)

def mongo_uri():
    if(mode == "prod"):
        return "mongodb://haichess-dev:jt9RfKmjOQd@haichess01.mongodb.rds.aliyuncs.com:3717/"
    else:
        return "mongodb://localhost:27017/"

def mongo_db():
    if(mode == "prod"):
        return "haichess-dev"
    else:
        return "lichess"

client = pymongo.MongoClient(mongo_uri())
db = client[mongo_db()]

num = 0
def importNextGame(puzzleId):
    global num
    num = num + 1
    logging.info("%s============start============%s", puzzleId, num)
    for game in findGame(puzzleId):
        post(game["game_id"], int(game["puzzle_id"]), game["game_pgn"])

def findGame(puzzleId):
    return db.candidate_games.find({
        "puzzle_id": {
            "$gt": puzzleId
        }
    }).sort("puzzle_id", 1).limit(1)

def post_uri():
    if(mode == "prod"):
        return "https://haichess.com/import/system"
    else:
        return "http://localhost/import/system"

def post(gameId, puzzleId, pgn):
    logging.info("Import Begin, GameID: %s, PuzzleID: %s", gameId, puzzleId)
    uri = post_uri() + "?secret=B15iz3lcF12OmQj32wQ6M9jpYqLyKBtN&gameId={gameId}&puzzleId={puzzleId}".format(gameId = gameId, puzzleId = puzzleId)
    headers = {
            "X-Requested-With": "XMLHttpRequest",
            "Content-Type": "application/x-www-form-urlencoded"
        }
    data = {
            "pgn": pgn,
            "analyse": "true"
        }
    try:
        response = requests.post(uri, headers = headers, data = data)
        if response.status_code == 200:
            logging.info("Import Over, GameID: %s, PuzzleID: %s", gameId, puzzleId)
            importNextGame(puzzleId)
        else:
            logging.error("Unexpected HTTP status for abort: %d, GameID: %s, PuzzleID: %s", response.status_code, gameId, puzzleId)
    except requests.RequestException:
        logging.exception("Could not Post Game, GameID: %s, PuzzleID: %s", gameId, puzzleId)


importNextGame(int(puzzleIDForm))
