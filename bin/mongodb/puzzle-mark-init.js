db.adm_puzzle_mark.update({},
    {
        $set:{
            "mark.publishStatus" : "Unpublished",
            "mark.validStatus": "Save",
            "mark.markStatus": "Unmarked"

        },
        $unset:{
            "mark.time": "",
            "mark.phase": "",
            "mark.validStatusCause": "",
            "mark.strength": [],
            "mark.moveFor": [],
            "mark.usePiece": [],
            "mark.subject": [],
            "mark.chessGame": [],
            "mark.comprehensive": [],
            "mark.tag": []
        }
    },
    { multi: true, upsert: false}
)